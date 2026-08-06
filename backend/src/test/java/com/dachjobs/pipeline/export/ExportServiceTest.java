package com.dachjobs.pipeline.export;

import com.dachjobs.pipeline.export.GoldExportRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Exercises the validation gates ported from the assert statements in
 * notebooks/05_gold_aggregate.py and notebooks/06_publish_to_github.py:
 * ExportService.run must refuse to publish a broken snapshot rather than
 * silently shipping bad data.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final long RULESET_ID = 1L;
    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 8, 5);

    @Mock
    private GoldAggregationService aggregation;

    private ExportService service;

    @BeforeEach
    void setUp() {
        ExportProperties props = new ExportProperties();
        props.setOutputDir(System.getProperty("java.io.tmpdir") + "/export-service-test-" + System.nanoTime());
        service = new ExportService(aggregation, props);
    }

    private MarketSummary marketSummary(long livePostings) {
        return new MarketSummary(SNAPSHOT, livePostings, 100, 40.0, 20.0, 80.0, 50.0, 25.0, 10.0, 20.0, 60.0);
    }

    /** Stubs everything a healthy run needs so a single gate can be broken at a time. */
    private void stubHealthyRun(long kept) {
        lenient().when(aggregation.marketSummary(RULESET_ID, SNAPSHOT)).thenReturn(marketSummary(kept));
        lenient().when(aggregation.quarantinedCount(RULESET_ID, SNAPSHOT)).thenReturn(10L);
        lenient().when(aggregation.maxAgeDays(RULESET_ID, SNAPSHOT)).thenReturn(90);
        lenient().when(aggregation.ageDistribution(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.agencyComparison(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.skillDemand(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.countryBreakdown(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.cityBreakdown(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.staleByCompany(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.roleBreakdown(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.cityRoleBreakdown(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.skillByRole(RULESET_ID, SNAPSHOT)).thenReturn(List.of());
        lenient().when(aggregation.history(RULESET_ID)).thenReturn(List.of());
        lenient().when(aggregation.postingsPublic(RULESET_ID, SNAPSHOT)).thenReturn(publicPostings((int) kept));
        lenient().when(aggregation.descTruncatedPct(RULESET_ID, SNAPSHOT)).thenReturn(1.0);
    }

    private List<PostingPublicRow> publicPostings(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new PostingPublicRow("hash" + i, "title", "company", "berlin", "de",
                        "data", "data engineer", "mid", "en", false, 10, SNAPSHOT, "https://example.com/" + i, SNAPSHOT))
                .toList();
    }

    @Test
    void refusesToPublishWhenTooFewPostingsAreKept() {
        stubHealthyRun(500);

        assertThatThrownBy(() -> service.run(RULESET_ID, SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("500 postings");
    }

    @Test
    void refusesToPublishWhenQuarantineRateTooHigh() {
        stubHealthyRun(900);
        when(aggregation.quarantinedCount(RULESET_ID, SNAPSHOT)).thenReturn(200L);

        assertThatThrownBy(() -> service.run(RULESET_ID, SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quarantine rate");
    }

    @Test
    void refusesToPublishWhenMaxAgeIsImplausible() {
        stubHealthyRun(900);
        when(aggregation.maxAgeDays(RULESET_ID, SNAPSHOT)).thenReturn(2500);

        assertThatThrownBy(() -> service.run(RULESET_ID, SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("implausible age");
    }

    @Test
    void refusesToPublishWhenTooFewPostingsAreLinkable() {
        stubHealthyRun(900);
        when(aggregation.postingsPublic(RULESET_ID, SNAPSHOT)).thenReturn(publicPostings(100));

        assertThatThrownBy(() -> service.run(RULESET_ID, SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("linkable postings");
    }

    @Test
    void publishesAndReturnsSummaryWhenSnapshotIsHealthy() {
        stubHealthyRun(900);

        ExportService.ExportSummary summary = service.run(RULESET_ID, SNAPSHOT);

        assertThat(summary.kept()).isEqualTo(900);
        assertThat(summary.quarantined()).isEqualTo(10);
        assertThat(summary.postingsPublished()).isEqualTo(900);
    }
}
