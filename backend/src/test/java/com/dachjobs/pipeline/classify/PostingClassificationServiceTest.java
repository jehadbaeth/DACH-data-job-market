package com.dachjobs.pipeline.classify;

import com.dachjobs.pipeline.domain.Posting;
import com.dachjobs.pipeline.domain.RawPosting;
import com.dachjobs.pipeline.domain.RoleFamily;
import com.dachjobs.pipeline.repo.PostingRepository;
import com.dachjobs.pipeline.repo.PostingSkillRepository;
import com.dachjobs.pipeline.repo.RawPostingRepository;
import com.dachjobs.pipeline.repo.RoleFamilyRepository;
import com.dachjobs.pipeline.skills.SkillDef;
import com.dachjobs.pipeline.skills.SkillMatcher;
import com.dachjobs.pipeline.skills.SkillMatcherProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Exercises the ordering the original pipeline depends on: quality gate
 * first, dedup only within what passes it (notebooks/03_silver_clean.py),
 * then the published/unpublished scope filter decides KEPT vs EXCLUDED.
 */
@ExtendWith(MockitoExtension.class)
class PostingClassificationServiceTest {

    private static final Long RULESET_ID = 1L;
    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 8, 5);

    @Mock
    private RawPostingRepository rawPostingRepository;
    @Mock
    private RoleFamilyRepository roleFamilyRepository;
    @Mock
    private PostingRepository postingRepository;
    @Mock
    private PostingSkillRepository postingSkillRepository;

    private PostingClassificationService service;

    @BeforeEach
    void setUp() {
        List<ClassificationRuleDef> rules = List.of(
                new ClassificationRuleDef(0, "data engineer", "data engineer"),
                new ClassificationRuleDef(1, "ai (other)", "\\bai\\b"));
        RuleEngineProviderStub ruleEngineProvider = new RuleEngineProviderStub(new RuleEngine(rules));

        SkillMatcherProviderStub skillMatcherProvider = new SkillMatcherProviderStub(
                new SkillMatcher(List.of(new SkillDef("python", "language", List.of("\\bpython\\b"), null))));

        when(roleFamilyRepository.findByRulesetIdOrderBySortOrderAsc(RULESET_ID)).thenReturn(List.of(
                published("data engineer", "data"),
                unpublished("ai (other)", "excluded")));

        when(postingRepository.save(any(Posting.class))).thenAnswer(inv -> {
            Posting p = inv.getArgument(0);
            return p;
        });

        service = new PostingClassificationService(rawPostingRepository, ruleEngineProvider,
                roleFamilyRepository, postingRepository, skillMatcherProvider, postingSkillRepository);
    }

    private static RoleFamily published(String key, String group) {
        RoleFamily f = new RoleFamily();
        f.setRulesetId(RULESET_ID);
        f.setKey(key);
        f.setGroupName(group);
        f.setPublished(true);
        return f;
    }

    private static RoleFamily unpublished(String key, String group) {
        RoleFamily f = new RoleFamily();
        f.setRulesetId(RULESET_ID);
        f.setKey(key);
        f.setGroupName(group);
        f.setPublished(false);
        return f;
    }

    private static RawPosting raw(long id, String adzunaId, String title, String company, String city,
                                   String description, LocalDateTime created) {
        RawPosting r = new RawPosting();
        r.setAdzunaId(adzunaId);
        r.setCountry("de");
        r.setQueryRole("data engineer");
        r.setQueryPage(1);
        r.setPullDate(SNAPSHOT);
        r.setTitle(title);
        r.setCompany(company);
        r.setCityRaw(city);
        r.setArea2(city);
        r.setDescription(description);
        r.setRedirectUrl("https://example.com/" + adzunaId);
        r.setCreated(created);
        // reflectively set id since RawPosting has no public setter for it
        try {
            var f = RawPosting.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Test
    void keepsAPublishedFamilyAndExtractsItsSkills() {
        when(rawPostingRepository.findLatestPullDate()).thenReturn(Optional.of(SNAPSHOT));
        when(rawPostingRepository.findByPullDate(SNAPSHOT)).thenReturn(List.of(
                raw(1, "a1", "Data Engineer", "Acme GmbH", "Berlin",
                        "Python and Spark experience required.", LocalDateTime.of(2026, 7, 1, 9, 0))));

        PostingClassificationService.ClassificationSummary summary = service.run(RULESET_ID);

        assertThat(summary.kept()).isEqualTo(1);
        assertThat(summary.excluded()).isZero();
        assertThat(summary.quarantined()).isZero();

        ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
        verify(postingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("KEPT");
        assertThat(captor.getValue().getRoleFamilyKey()).isEqualTo("data engineer");

        verify(postingSkillRepository).save(argThat(ps -> ps.getSkillKey().equals("python")));
    }

    @Test
    void excludesAnUnpublishedFamilyWithTheFamilyKeyAsReason() {
        when(rawPostingRepository.findLatestPullDate()).thenReturn(Optional.of(SNAPSHOT));
        when(rawPostingRepository.findByPullDate(SNAPSHOT)).thenReturn(List.of(
                raw(1, "a1", "Fullstack Developer with AI focus", "Acme GmbH", "Berlin",
                        "We build products.", LocalDateTime.of(2026, 7, 1, 9, 0))));

        PostingClassificationService.ClassificationSummary summary = service.run(RULESET_ID);

        assertThat(summary.excluded()).isEqualTo(1);
        assertThat(summary.kept()).isZero();

        ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
        verify(postingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("EXCLUDED");
        assertThat(captor.getValue().getExclusionReason()).isEqualTo("ai (other)");
        verify(postingSkillRepository, never()).save(any());
    }

    @Test
    void quarantinesATitleThatFailsTheQualityGateWithoutDeduping() {
        when(rawPostingRepository.findLatestPullDate()).thenReturn(Optional.of(SNAPSHOT));
        when(rawPostingRepository.findByPullDate(SNAPSHOT)).thenReturn(List.of(
                raw(1, "a1", "Data Engineer", null /* no company */, "Berlin",
                        "Python experience.", LocalDateTime.of(2026, 7, 1, 9, 0))));

        PostingClassificationService.ClassificationSummary summary = service.run(RULESET_ID);

        assertThat(summary.quarantined()).isEqualTo(1);
        assertThat(summary.kept()).isZero();
        assertThat(summary.excluded()).isZero();

        ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
        verify(postingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("QUARANTINED");
        assertThat(captor.getValue().getExclusionReason()).contains("company_present");
    }

    @Test
    void dedupesTwoIdenticalListingsKeepingTheEarliestCreated() {
        when(rawPostingRepository.findLatestPullDate()).thenReturn(Optional.of(SNAPSHOT));
        when(rawPostingRepository.findByPullDate(SNAPSHOT)).thenReturn(List.of(
                raw(1, "a1", "Data Engineer", "Acme GmbH", "Berlin",
                        "Python experience.", LocalDateTime.of(2026, 7, 5, 9, 0)),
                raw(2, "a2", "Data Engineer", "Acme GmbH", "Berlin",
                        "Python experience.", LocalDateTime.of(2026, 7, 1, 9, 0))));

        PostingClassificationService.ClassificationSummary summary = service.run(RULESET_ID);

        assertThat(summary.kept()).isEqualTo(1);

        ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
        verify(postingRepository).save(captor.capture());
        assertThat(captor.getValue().getAdzunaId()).isEqualTo("a2");
        assertThat(captor.getValue().getCreatedDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    /** Avoids pulling in a real ClassificationRuleRepository/DB for this unit test. */
    private static class RuleEngineProviderStub extends RuleEngineProvider {
        private final RuleEngine engine;

        RuleEngineProviderStub(RuleEngine engine) {
            super(null);
            this.engine = engine;
        }

        @Override
        public RuleEngine forRuleset(Long rulesetId) {
            return engine;
        }
    }

    private static class SkillMatcherProviderStub extends SkillMatcherProvider {
        private final SkillMatcher matcher;

        SkillMatcherProviderStub(SkillMatcher matcher) {
            super(null, null);
            this.matcher = matcher;
        }

        @Override
        public SkillMatcher forRuleset(Long rulesetId) {
            return matcher;
        }
    }
}
