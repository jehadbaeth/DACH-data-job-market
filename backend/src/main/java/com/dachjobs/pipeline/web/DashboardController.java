package com.dachjobs.pipeline.web;

import com.dachjobs.pipeline.export.DashboardQueryService;
import com.dachjobs.pipeline.repo.RulesetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read-only access to a past pipeline run, for the frontend's snapshot date picker. */
@RestController
public class DashboardController {

    private final DashboardQueryService queryService;
    private final RulesetRepository rulesetRepository;

    public DashboardController(DashboardQueryService queryService, RulesetRepository rulesetRepository) {
        this.queryService = queryService;
        this.rulesetRepository = rulesetRepository;
    }

    @GetMapping("/api/dashboard/{rulesetKey}/dates")
    public List<LocalDate> dates(@PathVariable String rulesetKey) {
        return queryService.availableDates(rulesetIdFor(rulesetKey));
    }

    @GetMapping("/api/dashboard/{rulesetKey}/{date}")
    public Map<String, Object> snapshot(@PathVariable String rulesetKey, @PathVariable LocalDate date) {
        long rulesetId = rulesetIdFor(rulesetKey);
        if (!queryService.availableDates(rulesetId).contains(date)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no snapshot for date: " + date);
        }
        return queryService.snapshot(rulesetId, date);
    }

    private Long rulesetIdFor(String rulesetKey) {
        return rulesetRepository.findByKey(rulesetKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown ruleset: " + rulesetKey))
                .getId();
    }
}
