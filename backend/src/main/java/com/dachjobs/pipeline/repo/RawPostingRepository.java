package com.dachjobs.pipeline.repo;

import com.dachjobs.pipeline.domain.RawPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RawPostingRepository extends JpaRepository<RawPosting, Long> {
    List<RawPosting> findByPullDate(LocalDate pullDate);

    boolean existsByAdzunaIdAndPullDate(String adzunaId, LocalDate pullDate);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(r.pullDate) FROM RawPosting r")
    Optional<LocalDate> findLatestPullDate();
}
