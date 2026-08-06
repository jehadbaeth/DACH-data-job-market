package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ruleset")
public class Ruleset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String key;
    private String label;
    private String description;

    public Long getId() { return id; }
    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }

    public void setKey(String key) { this.key = key; }
    public void setLabel(String label) { this.label = label; }
    public void setDescription(String description) { this.description = description; }
}
