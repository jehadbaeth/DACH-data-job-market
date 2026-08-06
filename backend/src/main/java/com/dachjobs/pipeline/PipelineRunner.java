package com.dachjobs.pipeline;

import com.dachjobs.pipeline.classify.PostingClassificationService;
import com.dachjobs.pipeline.domain.Ruleset;
import com.dachjobs.pipeline.export.ExportService;
import com.dachjobs.pipeline.ingest.IngestService;
import com.dachjobs.pipeline.repo.PostingRepository;
import com.dachjobs.pipeline.repo.RulesetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Chains ingest -> classify -> export, the same weekly cadence the original
 * ran as separate Databricks notebook jobs. Ingest is global (one Adzuna
 * pull feeds every ruleset); classify and export run per ruleset so a new
 * vertical added as data (a new ruleset row) is picked up automatically,
 * and one ruleset's validation-gate failure doesn't block the others.
 */
@Service
public class PipelineRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final IngestService ingestService;
    private final PostingClassificationService classificationService;
    private final ExportService exportService;
    private final RulesetRepository rulesetRepository;
    private final PostingRepository postingRepository;

    public PipelineRunner(IngestService ingestService,
                           PostingClassificationService classificationService,
                           ExportService exportService,
                           RulesetRepository rulesetRepository,
                           PostingRepository postingRepository) {
        this.ingestService = ingestService;
        this.classificationService = classificationService;
        this.exportService = exportService;
        this.rulesetRepository = rulesetRepository;
        this.postingRepository = postingRepository;
    }

    public record RulesetRunResult(String rulesetKey, boolean success, String detail) {
    }

    public record PipelineRunSummary(IngestService.IngestSummary ingest, List<RulesetRunResult> rulesets) {
    }

    @Scheduled(cron = "${dachjobs.pipeline.cron:0 0 3 * * MON}")
    public void scheduledRun() {
        PipelineRunSummary summary = run(false);
        long failed = summary.rulesets().stream().filter(r -> !r.success()).count();
        if (failed > 0) {
            log.warn("Scheduled pipeline run finished with {} of {} rulesets failing", failed, summary.rulesets().size());
        } else {
            log.info("Scheduled pipeline run finished cleanly for {} ruleset(s)", summary.rulesets().size());
        }
    }

    /**
     * @param skipIngest true to re-run classify+export against whatever is
     *                    already in raw_posting, without calling the Adzuna
     *                    API again. Ingest dedupes what it *saves*, not what
     *                    it *fetches* - every call still spends a full page
     *                    walk of API quota - so skip it once quota is tight,
     *                    e.g. while iterating on classify/export during testing.
     */
    public PipelineRunSummary run(boolean skipIngest) {
        IngestService.IngestSummary ingestSummary = skipIngest ? null : ingestService.run();

        List<RulesetRunResult> results = rulesetRepository.findAll().stream()
                .map(this::runRuleset)
                .toList();

        return new PipelineRunSummary(ingestSummary, results);
    }

    private RulesetRunResult runRuleset(Ruleset ruleset) {
        try {
            classificationService.run(ruleset.getId());
            var snapshotDate = postingRepository.findLatestSnapshotDate(ruleset.getId())
                    .orElseThrow(() -> new IllegalStateException("no classified postings after classify"));
            exportService.run(ruleset.getId(), snapshotDate);
            return new RulesetRunResult(ruleset.getKey(), true, "ok");
        } catch (Exception e) {
            log.error("Pipeline run failed for ruleset '{}': {}", ruleset.getKey(), e.getMessage(), e);
            return new RulesetRunResult(ruleset.getKey(), false, e.getMessage());
        }
    }
}
