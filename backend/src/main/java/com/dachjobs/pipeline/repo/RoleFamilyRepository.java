package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.RoleFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleFamilyRepository extends JpaRepository<RoleFamily, Long> {
    List<RoleFamily> findByRulesetIdOrderBySortOrderAsc(Long rulesetId);
}
