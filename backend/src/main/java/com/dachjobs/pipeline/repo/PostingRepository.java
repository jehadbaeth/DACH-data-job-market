package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostingRepository extends JpaRepository<Posting, Long> {
    List<Posting> findByRulesetIdAndSnapshotDateAndStatus(Long rulesetId, LocalDate snapshotDate, String status);

    List<Posting> findByRulesetIdAndSnapshotDate(Long rulesetId, LocalDate snapshotDate);

    // Bulk JPQL delete, executed immediately when called - unlike a
    // no-@Modifying derived deleteBy method, which only *queues* entity
    // removals for the next flush and, with IDENTITY-generated ids, can
    // lose the race against inserts issued later in the same transaction.
    @Modifying
    @Transactional
    @Query("DELETE FROM Posting p WHERE p.rulesetId = :rulesetId AND p.snapshotDate = :snapshotDate")
    void deleteByRulesetIdAndSnapshotDate(@Param("rulesetId") Long rulesetId, @Param("snapshotDate") LocalDate snapshotDate);

    @Query("SELECT MAX(p.snapshotDate) FROM Posting p WHERE p.rulesetId = :rulesetId")
    Optional<LocalDate> findLatestSnapshotDate(@Param("rulesetId") Long rulesetId);
}
