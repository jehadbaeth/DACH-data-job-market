package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.HistoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface HistoryMetricRepository extends JpaRepository<HistoryMetric, Long> {

    List<HistoryMetric> findByRulesetIdOrderBySnapshotDateAscMetricAscDimensionAsc(Long rulesetId);

    @Transactional
    void deleteByRulesetIdAndSnapshotDate(Long rulesetId, LocalDate snapshotDate);
}
