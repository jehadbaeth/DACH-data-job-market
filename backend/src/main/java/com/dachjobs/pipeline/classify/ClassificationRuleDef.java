package com.dachjobs.pipeline.classify;

/**
 * Plain, DB-agnostic view of a classification_rule row, so {@link RuleEngine}
 * can be unit tested without a Spring context or a database.
 */
public record ClassificationRuleDef(int priority, String familyKey, String pattern) {
}
