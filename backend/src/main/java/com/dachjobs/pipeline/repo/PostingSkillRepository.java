package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.PostingSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostingSkillRepository extends JpaRepository<PostingSkill, Long> {
    List<PostingSkill> findByPostingId(Long postingId);
}
