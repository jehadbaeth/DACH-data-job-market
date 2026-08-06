package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill_alias")
public class SkillAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skillId;
    private String pattern;

    public Long getId() { return id; }
    public Long getSkillId() { return skillId; }
    public String getPattern() { return pattern; }

    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
