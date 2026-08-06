package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.SkillDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinition, Long> {
    List<SkillDefinition> findByRulesetId(Long rulesetId);
}
