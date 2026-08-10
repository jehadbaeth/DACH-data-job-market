package com.dachjobs.pipeline.export;

import com.dachjobs.pipeline.export.GoldExportRecords.*;
import com.dachjobs.pipeline.repo.PostingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only counterpart to ExportService for browsing a past pipeline run:
 * recomputes the same gold breakdowns for an arbitrary snapshot date still
 * present in `posting`, without ExportService's publish-gate checks or its
 * history_metric/file side effects.
 */
@Service
public class DashboardQueryService {

    private final GoldAggregationService aggregation;
    private final PostingRepository postingRepository;

    public DashboardQueryService(GoldAggregationService aggregation, PostingRepository postingRepository) {
        this.aggregation = aggregation;
        this.postingRepository = postingRepository;
    }

    public List<LocalDate> availableDates(long rulesetId) {
        return postingRepository.findDistinctSnapshotDates(rulesetId);
    }

    public Map<String, Object> snapshot(long rulesetId, LocalDate snapshotDate) {
        MarketSummary marketSummary = aggregation.marketSummary(rulesetId, snapshotDate);
        long quarantined = aggregation.quarantinedCount(rulesetId, snapshotDate);
        long kept = marketSummary.livePostings();
        double quarantineRate = (kept + quarantined) == 0 ? 0.0 : (double) quarantined / (kept + quarantined);

        List<PostingPublicRow> postings = aggregation.postingsPublic(rulesetId, snapshotDate);
        double descTruncatedPct = aggregation.descTruncatedPct(rulesetId, snapshotDate);

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("market_summary", List.of(marketSummary));
        bundle.put("age_distribution", cap(aggregation.ageDistribution(rulesetId, snapshotDate), 10));
        bundle.put("agency_comparison", cap(aggregation.agencyComparison(rulesetId, snapshotDate), 10));
        bundle.put("skill_demand", cap(aggregation.skillDemand(rulesetId, snapshotDate), 60));
        bundle.put("history", aggregation.history(rulesetId));
        bundle.put("country_breakdown", cap(aggregation.countryBreakdown(rulesetId, snapshotDate), 10));
        bundle.put("city_breakdown", aggregation.cityBreakdown(rulesetId, snapshotDate));
        bundle.put("stale_by_company", cap(aggregation.staleByCompany(rulesetId, snapshotDate), 60));
        bundle.put("role_breakdown", cap(aggregation.roleBreakdown(rulesetId, snapshotDate), 200));
        bundle.put("city_role_breakdown", cap(aggregation.cityRoleBreakdown(rulesetId, snapshotDate), 20000));
        bundle.put("skill_by_role", cap(aggregation.skillByRole(rulesetId, snapshotDate), 2000));
        bundle.put("postings", postings);
        bundle.put("meta", new Meta(snapshotDate, kept, marketSummary.employers(),
                marketSummary.avgAgeDays(), marketSummary.medianAgeDays(), marketSummary.pctOver60d(),
                quarantined, Math.round(quarantineRate * 10000) / 10000.0, postings.size(), descTruncatedPct));

        return bundle;
    }

    private static <T> List<T> cap(List<T> rows, int limit) {
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }
}
