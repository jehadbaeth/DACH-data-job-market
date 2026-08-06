package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.ClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {
    List<ClassificationRule> findByRulesetIdOrderByPriorityAsc(Long rulesetId);
}
