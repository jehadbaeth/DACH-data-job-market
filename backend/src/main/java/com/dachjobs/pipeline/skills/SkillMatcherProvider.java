package com.dachjobs.pipeline.skills;

import com.dachjobs.pipeline.domain.SkillAlias;
import com.dachjobs.pipeline.domain.SkillDefinition;
import com.dachjobs.pipeline.repo.SkillAliasRepository;
import com.dachjobs.pipeline.repo.SkillDefinitionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builds and caches one {@link SkillMatcher} per ruleset, mirroring
 * {@link com.dachjobs.pipeline.classify.RuleEngineProvider}. Each ruleset has
 * its own skill_definition rows, so a vertical's dictionary (e.g. software
 * languages/frameworks) never gets scanned against another vertical's
 * postings.
 */
@Component
public class SkillMatcherProvider {

    private final SkillDefinitionRepository skillRepository;
    private final SkillAliasRepository aliasRepository;
    private final Map<Long, SkillMatcher> cache = new ConcurrentHashMap<>();

    public SkillMatcherProvider(SkillDefinitionRepository skillRepository,
                                 SkillAliasRepository aliasRepository) {
        this.skillRepository = skillRepository;
        this.aliasRepository = aliasRepository;
    }

    public SkillMatcher forRuleset(Long rulesetId) {
        return cache.computeIfAbsent(rulesetId, this::build);
    }

    public void invalidate(Long rulesetId) {
        cache.remove(rulesetId);
    }

    private SkillMatcher build(Long rulesetId) {
        List<SkillDefinition> skills = skillRepository.findByRulesetId(rulesetId);
        List<Long> skillIds = skills.stream().map(SkillDefinition::getId).toList();
        Map<Long, List<String>> aliasesBySkillId = aliasRepository.findAll().stream()
                .filter(a -> skillIds.contains(a.getSkillId()))
                .collect(Collectors.groupingBy(SkillAlias::getSkillId,
                        Collectors.mapping(SkillAlias::getPattern, Collectors.toList())));

        List<SkillDef> defs = new ArrayList<>();
        for (SkillDefinition s : skills) {
            defs.add(new SkillDef(s.getKey(), s.getCategory(),
                    aliasesBySkillId.getOrDefault(s.getId(), List.of()),
                    s.getContextPattern()));
        }
        return new SkillMatcher(defs);
    }
}
