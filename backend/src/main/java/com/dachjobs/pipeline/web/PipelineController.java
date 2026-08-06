package com.dachjobs.pipeline.web;

import com.dachjobs.pipeline.PipelineRunner;
import com.dachjobs.pipeline.classify.PostingClassificationService;
import com.dachjobs.pipeline.export.ExportService;
import com.dachjobs.pipeline.ingest.IngestService;
import com.dachjobs.pipeline.repo.PostingRepository;
import com.dachjobs.pipeline.repo.RulesetRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Manual triggers for the individual pipeline steps and for a full run, on top of the weekly {@link PipelineRunner} schedule. */
@RestController
public class PipelineController {

    private final IngestService ingestService;
    private final PostingClassificationService classificationService;
    private final ExportService exportService;
    private final RulesetRepository rulesetRepository;
    private final PostingRepository postingRepository;
    private final PipelineRunner pipelineRunner;

    public PipelineController(IngestService ingestService,
                               PostingClassificationService classificationService,
                               ExportService exportService,
                               RulesetRepository rulesetRepository,
                               PostingRepository postingRepository,
                               PipelineRunner pipelineRunner) {
        this.ingestService = ingestService;
        this.classificationService = classificationService;
        this.exportService = exportService;
        this.rulesetRepository = rulesetRepository;
        this.postingRepository = postingRepository;
        this.pipelineRunner = pipelineRunner;
    }

    @PostMapping("/api/pipeline/ingest")
    public IngestService.IngestSummary ingest() {
        return ingestService.run();
    }

    @PostMapping("/api/pipeline/classify/{rulesetKey}")
    public PostingClassificationService.ClassificationSummary classify(@PathVariable String rulesetKey) {
        return classificationService.run(rulesetIdFor(rulesetKey));
    }

    @PostMapping("/api/pipeline/export/{rulesetKey}")
    public ExportService.ExportSummary export(@PathVariable String rulesetKey) {
        Long rulesetId = rulesetIdFor(rulesetKey);
        var snapshotDate = postingRepository.findLatestSnapshotDate(rulesetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "no classified postings for ruleset: " + rulesetKey));
        return exportService.run(rulesetId, snapshotDate);
    }

    /**
     * skipIngest=true re-runs classify+export against whatever is already
     * in raw_posting, without spending Adzuna API quota - useful while
     * iterating on classification/export logic during testing.
     */
    @PostMapping("/api/pipeline/run")
    public PipelineRunner.PipelineRunSummary run(@RequestParam(defaultValue = "false") boolean skipIngest) {
        return pipelineRunner.run(skipIngest);
    }

    private Long rulesetIdFor(String rulesetKey) {
        return rulesetRepository.findByKey(rulesetKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown ruleset: " + rulesetKey))
                .getId();
    }
}
