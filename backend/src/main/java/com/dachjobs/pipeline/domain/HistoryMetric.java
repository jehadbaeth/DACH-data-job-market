package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "history_metric")
public class HistoryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;
    private LocalDate snapshotDate;
    private String metric;
    private String dimension;
    private double value;

    public Long getId() { return id; }
    public Long getRulesetId() { return rulesetId; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public String getMetric() { return metric; }
    public String getDimension() { return dimension; }
    public double getValue() { return value; }

    public void setRulesetId(Long v) { this.rulesetId = v; }
    public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public void setMetric(String v) { this.metric = v; }
    public void setDimension(String v) { this.dimension = v; }
    public void setValue(double v) { this.value = v; }
}
