package com.dachjobs.pipeline;

import com.dachjobs.pipeline.classify.PostingClassificationService;
import com.dachjobs.pipeline.domain.Ruleset;
import com.dachjobs.pipeline.export.ExportService;
import com.dachjobs.pipeline.ingest.IngestService;
import com.dachjobs.pipeline.repo.PostingRepository;
import com.dachjobs.pipeline.repo.RulesetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies the chain (ingest once, then classify+export per ruleset) and,
 * critically, that one ruleset's failure doesn't stop the others - a bad
 * classification run or a validation-gate rejection in one vertical must
 * not block a healthy vertical's weekly publish.
 */
@ExtendWith(MockitoExtension.class)
class PipelineRunnerTest {

    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 8, 5);

    @Mock
    private IngestService ingestService;
    @Mock
    private PostingClassificationService classificationService;
    @Mock
    private ExportService exportService;
    @Mock
    private RulesetRepository rulesetRepository;
    @Mock
    private PostingRepository postingRepository;

    private PipelineRunner runner;

    private Ruleset ruleset(long id, String key) {
        Ruleset r = new Ruleset();
        r.setKey(key);
        try {
            var field = Ruleset.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(r, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @BeforeEach
    void setUp() {
        runner = new PipelineRunner(ingestService, classificationService, exportService,
                rulesetRepository, postingRepository);
    }

    @Test
    void runsIngestOnceThenClassifyAndExportForEveryRuleset() {
        when(ingestService.run()).thenReturn(new IngestService.IngestSummary(SNAPSHOT, 10, 5, 0));
        Ruleset dataAi = ruleset(1L, "data-ai");
        Ruleset software = ruleset(2L, "software-eng");
        when(rulesetRepository.findAll()).thenReturn(List.of(dataAi, software));
        when(postingRepository.findLatestSnapshotDate(any())).thenReturn(Optional.of(SNAPSHOT));

        PipelineRunner.PipelineRunSummary summary = runner.run(false);

        verify(ingestService, times(1)).run();
        verify(classificationService).run(1L);
        verify(classificationService).run(2L);
        verify(exportService).run(eq(1L), eq(SNAPSHOT));
        verify(exportService).run(eq(2L), eq(SNAPSHOT));
        assertThat(summary.rulesets()).extracting(PipelineRunner.RulesetRunResult::success)
                .containsExactly(true, true);
    }

    @Test
    void oneRulesetFailingDoesNotStopTheOthers() {
        when(ingestService.run()).thenReturn(new IngestService.IngestSummary(SNAPSHOT, 10, 5, 0));
        Ruleset broken = ruleset(1L, "broken");
        Ruleset healthy = ruleset(2L, "healthy");
        when(rulesetRepository.findAll()).thenReturn(List.of(broken, healthy));
        doThrow(new IllegalStateException("quarantine rate too high")).when(classificationService).run(1L);
        when(postingRepository.findLatestSnapshotDate(2L)).thenReturn(Optional.of(SNAPSHOT));

        PipelineRunner.PipelineRunSummary summary = runner.run(false);

        verify(exportService).run(eq(2L), eq(SNAPSHOT));
        verify(exportService, never()).run(eq(1L), any());
        assertThat(summary.rulesets()).hasSize(2);
        assertThat(summary.rulesets().get(0).success()).isFalse();
        assertThat(summary.rulesets().get(0).rulesetKey()).isEqualTo("broken");
        assertThat(summary.rulesets().get(1).success()).isTrue();
    }

    @Test
    void skipIngestNeverCallsAdzunaButStillClassifiesAndExports() {
        Ruleset dataAi = ruleset(1L, "data-ai");
        when(rulesetRepository.findAll()).thenReturn(List.of(dataAi));
        when(postingRepository.findLatestSnapshotDate(1L)).thenReturn(Optional.of(SNAPSHOT));

        PipelineRunner.PipelineRunSummary summary = runner.run(true);

        verify(ingestService, never()).run();
        verify(classificationService).run(1L);
        verify(exportService).run(eq(1L), eq(SNAPSHOT));
        assertThat(summary.ingest()).isNull();
        assertThat(summary.rulesets().get(0).success()).isTrue();
    }
}
