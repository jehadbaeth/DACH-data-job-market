package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill_definition")
public class SkillDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;
    private String key;
    private String category;
    private String label;
    private String contextPattern;

    public Long getId() { return id; }
    public Long getRulesetId() { return rulesetId; }
    public String getKey() { return key; }
    public String getCategory() { return category; }
    public String getLabel() { return label; }
    public String getContextPattern() { return contextPattern; }

    public void setRulesetId(Long rulesetId) { this.rulesetId = rulesetId; }
    public void setKey(String key) { this.key = key; }
    public void setCategory(String category) { this.category = category; }
    public void setLabel(String label) { this.label = label; }
    public void setContextPattern(String contextPattern) { this.contextPattern = contextPattern; }
}
