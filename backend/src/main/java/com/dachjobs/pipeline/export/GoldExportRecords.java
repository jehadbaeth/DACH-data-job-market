package com.dachjobs.pipeline.export;

import java.time.LocalDate;

/**
 * One record type per docs/data/*.json file, field names matching what
 * notebooks/06_publish_to_github.py actually shipped (it exports gold
 * tables verbatim via pandas to_json, so the column names ARE the JSON
 * keys the frontend reads).
 */
public final class GoldExportRecords {

    private GoldExportRecords() {
    }

    public record MarketSummary(
            LocalDate snapshotDate, long livePostings, long employers,
            Double avgAgeDays, Double medianAgeDays,
            Double pctOver7d, Double pctOver30d, Double pctOver60d, Double pctOver90d,
            Double pctAgency, Double pctEnglish) {
    }

    public record AgeDistributionRow(String bucket, int sortKey, long nPostings, double pct) {
    }

    public record StaleByCompanyRow(String company, boolean isAgency, long nPostings,
                                     Double avgAgeDays, Integer oldestDays, Double pctOver60d) {
    }

    public record AgencyComparisonRow(String posterType, long nPostings, double pct,
                                       Double avgAgeDays, Double medianAgeDays, Double pctOver60d) {
    }

    public record RoleBreakdownRow(String country, String roleGroup, String roleFamily, String seniority,
                                    long nPostings, Double avgAgeDays, Double medianAgeDays,
                                    Double pctOver7d, Double pctOver30d, Double pctOver60d, Double pctOver90d,
                                    Double pctEnglish) {
    }

    public record CityBreakdownRow(String country, String city, long nPostings,
                                    Double avgAgeDays, Double pctEnglish, Double pctAgency) {
    }

    public record CityRoleBreakdownRow(String country, String city, String roleFamily,
                                        long nPostings, Double avgAgeDays) {
    }

    public record SkillDemandRow(String skill, String skillCategory, long nPostings,
                                  Double pctPostings, Double avgAgeDays, LocalDate snapshotDate) {
    }

    public record SkillByRoleRow(String roleFamily, String skill, String skillCategory,
                                  long nPostings, long rolePostings, Double pctRolePostings,
                                  Double avgAgeDays, LocalDate snapshotDate) {
    }

    public record HistoryRow(LocalDate snapshotDate, String metric, String dimension, double value) {
    }

    public record CountryBreakdownRow(String country, long nPostings, long employers,
                                       Double avgAgeDays, Double medianAgeDays,
                                       Double pctOver30d, Double pctOver60d, Double pctOver90d,
                                       Double pctEnglish, Double pctWithSalary, Double pctAgency) {
    }

    public record PostingPublicRow(String postingId, String title, String company, String city,
                                    String country, String roleGroup, String roleFamily, String seniority,
                                    String language, boolean isAgency, Integer ageDays,
                                    LocalDate createdDate, String url, LocalDate snapshotDate) {
    }

    public record Meta(LocalDate updated, long livePostings, long employers,
                        Double avgAgeDays, Double medianAgeDays, Double pctOver60d,
                        long quarantined, double quarantineRate, long postingsPublished,
                        double descTruncatedPct) {
    }
}
