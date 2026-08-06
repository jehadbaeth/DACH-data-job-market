package com.dachjobs.pipeline.export;

import com.dachjobs.pipeline.export.GoldExportRecords.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Writes the same docs/data/*.json files notebooks/06_publish_to_github.py
 * committed to GitHub, straight to disk here since the frontend just reads
 * whatever is in that folder - no GitHub API round trip needed locally.
 *
 * Reproduces the original's validation gate: refuses to write anything if
 * the snapshot looks broken, rather than quietly publishing bad data.
 */
@Service
public class ExportService {

    private final GoldAggregationService aggregation;
    private final ExportProperties exportProperties;
    private final ObjectMapper prettyMapper;
    private final ObjectMapper compactMapper;

    public ExportService(GoldAggregationService aggregation, ExportProperties exportProperties) {
        this.aggregation = aggregation;
        this.exportProperties = exportProperties;

        ObjectMapper base = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        this.prettyMapper = base.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.compactMapper = base.copy().disable(SerializationFeature.INDENT_OUTPUT);
    }

    public record ExportSummary(LocalDate snapshotDate, long kept, long quarantined,
                                 double quarantineRate, long postingsPublished) {
    }

    public ExportSummary run(long rulesetId, LocalDate snapshotDate) {
        MarketSummary marketSummary = aggregation.marketSummary(rulesetId, snapshotDate);
        long quarantined = aggregation.quarantinedCount(rulesetId, snapshotDate);
        long kept = marketSummary.livePostings();

        if (kept <= 500) {
            throw new IllegalStateException("only " + kept + " postings, refusing to publish");
        }
        double quarantineRate = (double) quarantined / (kept + quarantined);
        if (quarantineRate >= 0.10) {
            throw new IllegalStateException(
                    "quarantine rate %.1f%% too high".formatted(quarantineRate * 100));
        }
        Integer ageMax = aggregation.maxAgeDays(rulesetId, snapshotDate);
        if (ageMax != null && ageMax >= 2000) {
            throw new IllegalStateException("implausible age " + ageMax + ", check date parsing");
        }

        List<AgeDistributionRow> ageDistribution = aggregation.ageDistribution(rulesetId, snapshotDate);
        List<AgencyComparisonRow> agencyComparison = aggregation.agencyComparison(rulesetId, snapshotDate);
        List<SkillDemandRow> skillDemand = aggregation.skillDemand(rulesetId, snapshotDate);
        List<CountryBreakdownRow> countryBreakdown = aggregation.countryBreakdown(rulesetId, snapshotDate);
        List<CityBreakdownRow> cityBreakdown = aggregation.cityBreakdown(rulesetId, snapshotDate);
        List<StaleByCompanyRow> staleByCompany = aggregation.staleByCompany(rulesetId, snapshotDate);
        List<RoleBreakdownRow> roleBreakdown = aggregation.roleBreakdown(rulesetId, snapshotDate);
        List<CityRoleBreakdownRow> cityRoleBreakdown = aggregation.cityRoleBreakdown(rulesetId, snapshotDate);
        List<SkillByRoleRow> skillByRole = aggregation.skillByRole(rulesetId, snapshotDate);

        aggregation.recordHistory(rulesetId, snapshotDate, marketSummary, skillDemand, roleBreakdown);
        List<HistoryRow> history = aggregation.history(rulesetId);

        List<PostingPublicRow> postings = aggregation.postingsPublic(rulesetId, snapshotDate);
        if (postings.size() <= 500) {
            throw new IllegalStateException("only " + postings.size() + " linkable postings, refusing to ship");
        }

        write("market_summary.json", prettyMapper, List.of(marketSummary));
        write("age_distribution.json", prettyMapper, cap(ageDistribution, 10));
        write("agency_comparison.json", prettyMapper, cap(agencyComparison, 10));
        write("skill_demand.json", prettyMapper, cap(skillDemand, 60));
        write("history.json", prettyMapper, history);
        write("country_breakdown.json", prettyMapper, cap(countryBreakdown, 10));
        write("city_breakdown.json", prettyMapper, cityBreakdown);
        write("stale_by_company.json", prettyMapper, cap(staleByCompany, 60));
        write("role_breakdown.json", prettyMapper, cap(roleBreakdown, 200));
        write("city_role_breakdown.json", prettyMapper, cap(cityRoleBreakdown, 20000));
        write("skill_by_role.json", prettyMapper, cap(skillByRole, 2000));
        write("postings.json", compactMapper, postings);

        double descTruncatedPct = aggregation.descTruncatedPct(rulesetId, snapshotDate);
        Meta meta = new Meta(snapshotDate, kept, marketSummary.employers(), marketSummary.avgAgeDays(),
                marketSummary.medianAgeDays(), marketSummary.pctOver60d(), quarantined,
                Math.round(quarantineRate * 10000) / 10000.0, postings.size(), descTruncatedPct);
        write("meta.json", prettyMapper, meta);

        return new ExportSummary(snapshotDate, kept, quarantined, quarantineRate, postings.size());
    }

    private static <T> List<T> cap(List<T> rows, int limit) {
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    private void write(String filename, ObjectMapper mapper, Object payload) {
        try {
            Path dir = Path.of(exportProperties.getOutputDir());
            Files.createDirectories(dir);
            mapper.writeValue(dir.resolve(filename).toFile(), payload);
        } catch (IOException e) {
            throw new UncheckedIOException("failed writing " + filename, e);
        }
    }
}
