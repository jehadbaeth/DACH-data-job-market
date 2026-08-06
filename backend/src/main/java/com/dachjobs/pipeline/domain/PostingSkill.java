package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "posting_skill")
public class PostingSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postingId;
    private String skillKey;
    private String skillCategory;

    public Long getId() { return id; }
    public Long getPostingId() { return postingId; }
    public String getSkillKey() { return skillKey; }
    public String getSkillCategory() { return skillCategory; }

    public void setPostingId(Long v) { this.postingId = v; }
    public void setSkillKey(String v) { this.skillKey = v; }
    public void setSkillCategory(String v) { this.skillCategory = v; }
}
