package com.dachjobs.pipeline.classify;

import com.dachjobs.pipeline.domain.Posting;
import com.dachjobs.pipeline.domain.PostingSkill;
import com.dachjobs.pipeline.domain.RawPosting;
import com.dachjobs.pipeline.domain.RoleFamily;
import com.dachjobs.pipeline.repo.PostingRepository;
import com.dachjobs.pipeline.repo.PostingSkillRepository;
import com.dachjobs.pipeline.repo.RawPostingRepository;
import com.dachjobs.pipeline.repo.RoleFamilyRepository;
import com.dachjobs.pipeline.skills.SkillMatcher;
import com.dachjobs.pipeline.skills.SkillMatcherProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ports notebooks/03_silver_clean.py end to end: take the most recent
 * ingested snapshot, classify every title, run the quality gate, dedup the
 * rows that pass it (keeping the earliest listing per posting hash so
 * age_days measures true time-on-market), apply the published/unpublished
 * scope filter, and - for whatever is KEPT - extract skills.
 *
 * Order matters and is copied verbatim from the original: quality gate
 * first, dedup only within what passes it. A quarantined row is never
 * deduplicated, so its exclusion is fully auditable rather than silently
 * merged away.
 */
@Service
public class PostingClassificationService {

    private static final Logger log = LoggerFactory.getLogger(PostingClassificationService.class);

    private final RawPostingRepository rawPostingRepository;
    private final RuleEngineProvider ruleEngineProvider;
    private final RoleFamilyRepository roleFamilyRepository;
    private final PostingRepository postingRepository;
    private final SkillMatcherProvider skillMatcherProvider;
    private final PostingSkillRepository postingSkillRepository;

    public PostingClassificationService(RawPostingRepository rawPostingRepository,
                                         RuleEngineProvider ruleEngineProvider,
                                         RoleFamilyRepository roleFamilyRepository,
                                         PostingRepository postingRepository,
                                         SkillMatcherProvider skillMatcherProvider,
                                         PostingSkillRepository postingSkillRepository) {
        this.rawPostingRepository = rawPostingRepository;
        this.ruleEngineProvider = ruleEngineProvider;
        this.roleFamilyRepository = roleFamilyRepository;
        this.postingRepository = postingRepository;
        this.skillMatcherProvider = skillMatcherProvider;
        this.postingSkillRepository = postingSkillRepository;
    }

    public record ClassificationSummary(LocalDate snapshotDate, int kept, int excluded, int quarantined) {
    }

    @Transactional
    public ClassificationSummary run(Long rulesetId) {
        LocalDate snapshotDate = rawPostingRepository.findLatestPullDate().orElse(null);
        if (snapshotDate == null) {
            return new ClassificationSummary(null, 0, 0, 0);
        }

        List<RawPosting> rawPostings = rawPostingRepository.findByPullDate(snapshotDate);
        RuleEngine engine = ruleEngineProvider.forRuleset(rulesetId);
        Map<String, RoleFamily> familiesByKey = roleFamilyRepository
                .findByRulesetIdOrderBySortOrderAsc(rulesetId).stream()
                .collect(Collectors.toMap(RoleFamily::getKey, Function.identity()));

        List<PostingDraft> drafts = rawPostings.stream()
                .map(r -> buildDraft(r, engine, snapshotDate))
                .toList();

        postingRepository.deleteByRulesetIdAndSnapshotDate(rulesetId, snapshotDate);

        int kept = 0, excluded = 0, quarantined = 0;

        List<PostingDraft> passing = new ArrayList<>();
        for (PostingDraft draft : drafts) {
            List<String> failedRules = QualityGate.failedRules(draft, snapshotDate);
            if (!failedRules.isEmpty()) {
                persist(rulesetId, draft, familiesByKey, "QUARANTINED", String.join(",", failedRules));
                quarantined++;
            } else {
                passing.add(draft);
            }
        }

        for (PostingDraft draft : dedupKeepingEarliest(passing)) {
            RoleFamily family = familiesByKey.get(draft.roleFamilyKey());
            boolean published = family != null && family.isPublished();
            String status = published ? "KEPT" : "EXCLUDED";
            String exclusionReason = published ? null : draft.roleFamilyKey();
            Posting saved = persist(rulesetId, draft, familiesByKey, status, exclusionReason);
            if (published) {
                extractAndSaveSkills(rulesetId, saved);
                kept++;
            } else {
                excluded++;
            }
        }

        log.info("Classified snapshot {}: {} kept, {} excluded, {} quarantined",
                snapshotDate, kept, excluded, quarantined);
        return new ClassificationSummary(snapshotDate, kept, excluded, quarantined);
    }

    /** Same rule as the original's row_number().over(partitionBy(hash).orderBy(created asc)): keep rn=1. */
    private static List<PostingDraft> dedupKeepingEarliest(List<PostingDraft> drafts) {
        Map<String, PostingDraft> earliestByHash = new LinkedHashMap<>();
        for (PostingDraft d : drafts) {
            earliestByHash.merge(d.postingHash(), d,
                    (existing, candidate) -> candidate.createdDate().isBefore(existing.createdDate())
                            ? candidate : existing);
        }
        return List.copyOf(earliestByHash.values());
    }

    private PostingDraft buildDraft(RawPosting r, RuleEngine engine, LocalDate snapshotDate) {
        String titleRaw = r.getTitle() == null ? null : r.getTitle().trim();
        String titleNorm = TitleNormalizer.normalize(titleRaw);
        String roleFamilyKey = engine.classify(titleNorm);

        String company = r.getCompany() == null ? null : r.getCompany().trim();
        String companyNorm = TitleNormalizer.normalize(company);

        CityNormalizer.Result cityResult = CityNormalizer.normalize(r.getArea2(), r.getArea3());

        String description = r.getDescription() == null ? "" : r.getDescription();

        LocalDate createdDate = r.getCreated() == null ? null : r.getCreated().toLocalDate();
        Integer ageDays = createdDate == null ? null : (int) ChronoUnit.DAYS.between(createdDate, snapshotDate);

        return new PostingDraft(
                r.getId(),
                PostingHasher.hash(titleRaw, company, cityResult.city(), description),
                r.getAdzunaId(),
                r.getCountry(),
                titleRaw,
                titleNorm,
                roleFamilyKey,
                SeniorityDetector.detect(titleNorm),
                GenderedTagDetector.hasTag(titleNorm),
                company,
                companyNorm,
                AgencyDetector.isAgency(companyNorm),
                cityResult.city(),
                r.getCityRaw(),
                cityResult.region(),
                r.getCategory(),
                LanguageDetector.detect(description),
                r.getRedirectUrl(),
                description,
                description.length(),
                description.endsWith("…"),
                r.getSalaryMin(),
                r.getSalaryMax(),
                r.getSalaryIsPredicted(),
                r.getContractType(),
                r.getContractTime(),
                createdDate,
                ageDays,
                snapshotDate,
                r.getQueryRole());
    }

    private Posting persist(Long rulesetId, PostingDraft d, Map<String, RoleFamily> familiesByKey,
                             String status, String exclusionReason) {
        RoleFamily family = familiesByKey.get(d.roleFamilyKey());

        Posting p = new Posting();
        p.setRulesetId(rulesetId);
        p.setPostingHash(d.postingHash());
        p.setDedupHash("QUARANTINED".equals(status) ? null : d.postingHash());
        p.setRawPostingId(d.rawPostingId());
        p.setAdzunaId(d.adzunaId());
        p.setCountry(d.country());
        p.setTitleRaw(d.titleRaw());
        p.setTitleNorm(d.titleNorm());
        p.setRoleFamilyKey(d.roleFamilyKey());
        p.setRoleGroup(family != null ? family.getGroupName() : "excluded");
        p.setSeniority(d.seniority());
        p.setGenderedTag(d.genderedTag());
        p.setCompany(d.company());
        p.setCompanyNorm(d.companyNorm());
        p.setAgency(d.isAgency());
        p.setCity(d.city());
        p.setCityRaw(d.cityRaw());
        p.setRegion(d.region());
        p.setCategory(d.category());
        p.setLanguage(d.language());
        p.setRedirectUrl(d.redirectUrl());
        p.setDescription(d.description());
        p.setDescChars(d.descChars());
        p.setDescTruncated(d.descTruncated());
        p.setSalaryMin(d.salaryMin());
        p.setSalaryMax(d.salaryMax());
        p.setSalaryIsPredicted(d.salaryIsPredicted());
        p.setContractType(d.contractType());
        p.setContractTime(d.contractTime());
        p.setCreatedDate(d.createdDate());
        p.setAgeDays(d.ageDays());
        p.setSnapshotDate(d.snapshotDate());
        p.setQueryRole(d.queryRole());
        p.setStatus(status);
        p.setExclusionReason(exclusionReason);

        return postingRepository.save(p);
    }

    private void extractAndSaveSkills(Long rulesetId, Posting posting) {
        SkillMatcher matcher = skillMatcherProvider.forRuleset(rulesetId);
        List<String> skillKeys = matcher.extract(posting.getDescription());
        Map<String, String> categories = matcher.categoriesOf(skillKeys);
        for (String key : skillKeys) {
            PostingSkill ps = new PostingSkill();
            ps.setPostingId(posting.getId());
            ps.setSkillKey(key);
            ps.setSkillCategory(categories.get(key));
            postingSkillRepository.save(ps);
        }
    }
}
