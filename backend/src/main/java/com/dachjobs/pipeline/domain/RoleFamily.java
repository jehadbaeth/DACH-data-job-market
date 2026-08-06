package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_family")
public class RoleFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;
    private String key;
    private String label;
    private String groupName;
    private int sortOrder;
    private boolean published;

    public Long getId() { return id; }
    public Long getRulesetId() { return rulesetId; }
    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getGroupName() { return groupName; }
    public int getSortOrder() { return sortOrder; }
    public boolean isPublished() { return published; }

    public void setRulesetId(Long rulesetId) { this.rulesetId = rulesetId; }
    public void setKey(String key) { this.key = key; }
    public void setLabel(String label) { this.label = label; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setPublished(boolean published) { this.published = published; }
}
