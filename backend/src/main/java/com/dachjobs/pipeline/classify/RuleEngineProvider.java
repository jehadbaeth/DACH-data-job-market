package com.dachjobs.pipeline.classify;

import com.dachjobs.pipeline.domain.ClassificationRule;
import com.dachjobs.pipeline.repo.ClassificationRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and caches one {@link RuleEngine} per ruleset. Rules are read once
 * per ruleset id and reused; call {@link #invalidate(Long)} after editing
 * classification_rule rows for a ruleset (e.g. from an admin endpoint) so the
 * next classification run picks up the change without a restart.
 */
@Component
public class RuleEngineProvider {

    private final ClassificationRuleRepository ruleRepository;
    private final Map<Long, RuleEngine> cache = new ConcurrentHashMap<>();

    public RuleEngineProvider(ClassificationRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public RuleEngine forRuleset(Long rulesetId) {
        return cache.computeIfAbsent(rulesetId, this::build);
    }

    public void invalidate(Long rulesetId) {
        cache.remove(rulesetId);
    }

    private RuleEngine build(Long rulesetId) {
        List<ClassificationRuleDef> defs = ruleRepository
                .findByRulesetIdOrderByPriorityAsc(rulesetId)
                .stream()
                .map(this::toDef)
                .toList();
        return new RuleEngine(defs);
    }

    private ClassificationRuleDef toDef(ClassificationRule r) {
        return new ClassificationRuleDef(r.getPriority(), r.getFamilyKey(), r.getPattern());
    }
}
