package com.dachjobs.pipeline.ingest;

import com.dachjobs.pipeline.domain.RawPosting;
import com.dachjobs.pipeline.repo.RawPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Direct port of the pull loop in notebooks/01_ingest_adzuna.py: for every
 * country/role, walk pages until a page comes back with fewer than
 * results-per-page results (the natural end of that role's results) or
 * max-pages is hit, whichever first. Every result becomes one append-only
 * raw_posting row - the bronze layer is never updated or deleted.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final AdzunaClient client;
    private final AdzunaProperties properties;
    private final RawPostingRepository rawPostingRepository;

    public IngestService(AdzunaClient client, AdzunaProperties properties,
                          RawPostingRepository rawPostingRepository) {
        this.client = client;
        this.properties = properties;
        this.rawPostingRepository = rawPostingRepository;
    }

    public IngestSummary run() {
        LocalDate pullDate = LocalDate.now();
        int callsMade = 0;
        int postingsSaved = 0;
        int postingsSkippedAlreadyPulledToday = 0;

        for (String country : properties.getCountries()) {
            for (String role : properties.getRoles()) {
                for (int page = 1; page <= properties.getMaxPages(); page++) {
                    AdzunaSearchResponse response;
                    try {
                        response = client.fetch(country, role, page);
                    } catch (Exception e) {
                        log.error("Ingest failed for {}/{} page {}, skipping to next page: {}",
                                country, role, page, e.getMessage(), e);
                        continue;
                    }
                    callsMade++;

                    List<AdzunaSearchResponse.Result> results = response.getResults();
                    for (AdzunaSearchResponse.Result r : results) {
                        if (rawPostingRepository.existsByAdzunaIdAndPullDate(r.getId(), pullDate)) {
                            postingsSkippedAlreadyPulledToday++;
                            continue;
                        }
                        rawPostingRepository.save(toEntity(r, country, role, page, pullDate));
                        postingsSaved++;
                    }

                    if (page == 1) {
                        log.info("{} {} total={} page1={}", country, role, response.getCount(), results.size());
                    }

                    if (results.size() < properties.getResultsPerPage()) {
                        break;
                    }

                    politePause();
                }
            }
        }

        log.info("Ingest done: {} API calls, {} postings saved, {} already-pulled-today skipped",
                callsMade, postingsSaved, postingsSkippedAlreadyPulledToday);
        return new IngestSummary(pullDate, callsMade, postingsSaved, postingsSkippedAlreadyPulledToday);
    }

    private static void politePause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static RawPosting toEntity(AdzunaSearchResponse.Result r, String country, String role,
                                        int page, LocalDate pullDate) {
        RawPosting entity = new RawPosting();
        entity.setAdzunaId(r.getId());
        entity.setCountry(country);
        entity.setQueryRole(role);
        entity.setQueryPage(page);
        entity.setPullDate(pullDate);
        entity.setTitle(r.getTitle());
        entity.setDescription(r.getDescription());
        entity.setRedirectUrl(r.getRedirectUrl());
        entity.setSalaryMin(r.getSalaryMin());
        entity.setSalaryMax(r.getSalaryMax());
        entity.setSalaryIsPredicted("1".equals(r.getSalaryIsPredicted()));
        entity.setContractType(r.getContractType());
        entity.setContractTime(r.getContractTime());

        if (r.getCompany() != null) {
            entity.setCompany(r.getCompany().getDisplayName());
        }
        if (r.getLocation() != null) {
            entity.setCityRaw(r.getLocation().getDisplayName());
            List<String> area = r.getLocation().getArea();
            entity.setArea2(elementAt(area, 1));
            entity.setArea3(elementAt(area, 2));
        }
        if (r.getCategory() != null) {
            entity.setCategory(r.getCategory().getLabel());
        }
        entity.setCreated(parseCreated(r.getCreated()));

        return entity;
    }

    private static String elementAt(List<String> list, int index) {
        return list != null && index < list.size() ? list.get(index) : null;
    }

    private static LocalDateTime parseCreated(String created) {
        if (created == null || created.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
    }

    public record IngestSummary(LocalDate pullDate, int callsMade, int postingsSaved,
                                 int postingsSkippedAlreadyPulledToday) {
    }
}
