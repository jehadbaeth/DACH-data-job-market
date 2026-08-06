package com.dachjobs.pipeline.classify;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Generic mirror of the F.when() chain in notebooks/03_silver_clean.py:
 * rules are evaluated in priority order, first match wins. Whatever domain
 * the ruleset describes (DACH data/AI jobs today, software engineering by
 * language tomorrow) is entirely a property of the rules it was built with,
 * not of this class.
 *
 * Titles must be pre-normalized with {@link TitleNormalizer} before calling
 * {@link #classify(String)} — the original pipeline folds and lowercases
 * before matching, so patterns never need an inline case-insensitive flag.
 */
public final class RuleEngine {

    public static final String DEFAULT_FAMILY_KEY = "other";

    private final List<CompiledRule> rules;

    public RuleEngine(List<ClassificationRuleDef> ruleDefs) {
        this.rules = ruleDefs.stream()
                .sorted((a, b) -> Integer.compare(a.priority(), b.priority()))
                .map(d -> new CompiledRule(d.familyKey(), Pattern.compile(d.pattern())))
                .toList();
    }

    /**
     * @param normalizedTitle output of {@link TitleNormalizer#normalize(String)}
     * @return the key of the first matching family, or {@link #DEFAULT_FAMILY_KEY}
     */
    public String classify(String normalizedTitle) {
        for (CompiledRule rule : rules) {
            if (rule.pattern.matcher(normalizedTitle).find()) {
                return rule.familyKey;
            }
        }
        return DEFAULT_FAMILY_KEY;
    }

    private record CompiledRule(String familyKey, Pattern pattern) {
    }
}
