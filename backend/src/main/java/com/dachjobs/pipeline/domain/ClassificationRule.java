package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classification_rule")
public class ClassificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;
    private int priority;
    private String familyKey;
    private String pattern;
    private String description;

    public Long getId() { return id; }
    public Long getRulesetId() { return rulesetId; }
    public int getPriority() { return priority; }
    public String getFamilyKey() { return familyKey; }
    public String getPattern() { return pattern; }
    public String getDescription() { return description; }

    public void setRulesetId(Long rulesetId) { this.rulesetId = rulesetId; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setFamilyKey(String familyKey) { this.familyKey = familyKey; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public void setDescription(String description) { this.description = description; }
}
