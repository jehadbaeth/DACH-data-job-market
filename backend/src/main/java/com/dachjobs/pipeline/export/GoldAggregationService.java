package com.dachjobs.pipeline.export;

import com.dachjobs.pipeline.domain.HistoryMetric;
import com.dachjobs.pipeline.export.GoldExportRecords.*;
import com.dachjobs.pipeline.repo.HistoryMetricRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Recomputes every gold table from `posting`/`posting_skill` for one
 * ruleset's latest snapshot. Ports notebooks/05_gold_aggregate.py table
 * by table - the SQL here is the same aggregation, just against Postgres
 * instead of a Spark temp view.
 *
 * Every WHERE-clause predicate is repeated inline at each place it is
 * needed instead of factored into a CTE referenced more than once: H2
 * (used for tests) mishandles bind parameters inside a non-recursive CTE
 * that is joined against more than once, silently evaluating the second
 * reference as unfiltered. Repeating the predicate is the only form both
 * H2 and Postgres agree on.
 */
@Service
public class GoldAggregationService {

    private final JdbcTemplate jdbc;
    private final HistoryMetricRepository historyMetricRepository;

    public GoldAggregationService(JdbcTemplate jdbc, HistoryMetricRepository historyMetricRepository) {
        this.jdbc = jdbc;
        this.historyMetricRepository = historyMetricRepository;
    }

    private static final String KEPT = "status = 'KEPT'";

    private static Double dbl(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        // pgjdbc doesn't support getObject(col, Double.class) for numeric
        // columns (throws "conversion to class java.lang.Double from
        // numeric not supported") - BigDecimal is the type both H2 and the
        // real Postgres driver agree on for NUMERIC/DECIMAL.
        java.math.BigDecimal v = rs.getBigDecimal(col);
        return v == null ? null : v.doubleValue();
    }

    private static Integer integer(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        return rs.getObject(col, Integer.class);
    }

    public MarketSummary marketSummary(long rulesetId, LocalDate snapshotDate) {
        return jdbc.queryForObject("""
                SELECT
                  COUNT(*) AS live_postings,
                  COUNT(DISTINCT company) AS employers,
                  ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY age_days) AS median_age_days,
                  ROUND(100.0 * AVG(CASE WHEN age_days > 7 THEN 1 ELSE 0 END), 1) AS pct_over_7d,
                  ROUND(100.0 * AVG(CASE WHEN age_days > 30 THEN 1 ELSE 0 END), 1) AS pct_over_30d,
                  ROUND(100.0 * AVG(CASE WHEN age_days > 60 THEN 1 ELSE 0 END), 1) AS pct_over_60d,
                  ROUND(100.0 * AVG(CASE WHEN age_days > 90 THEN 1 ELSE 0 END), 1) AS pct_over_90d,
                  ROUND(100.0 * AVG(CASE WHEN is_agency THEN 1 ELSE 0 END), 1) AS pct_agency,
                  ROUND(100.0 * AVG(CASE WHEN language = 'en' THEN 1 ELSE 0 END), 1) AS pct_english
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                """.formatted(KEPT),
                (rs, i) -> new MarketSummary(snapshotDate, rs.getLong("live_postings"), rs.getLong("employers"),
                        dbl(rs, "avg_age_days"), dbl(rs, "median_age_days"),
                        dbl(rs, "pct_over_7d"), dbl(rs, "pct_over_30d"),
                        dbl(rs, "pct_over_60d"), dbl(rs, "pct_over_90d"),
                        dbl(rs, "pct_agency"), dbl(rs, "pct_english")),
                rulesetId, snapshotDate);
    }

    public List<AgeDistributionRow> ageDistribution(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT bucket, sort_key, COUNT(*) AS n_postings,
                       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 1) AS pct
                FROM (
                  SELECT CASE
                    WHEN age_days <= 7  THEN '0-7 days'
                    WHEN age_days <= 14 THEN '8-14 days'
                    WHEN age_days <= 30 THEN '15-30 days'
                    WHEN age_days <= 60 THEN '31-60 days'
                    WHEN age_days <= 90 THEN '61-90 days'
                    ELSE '90+ days' END AS bucket,
                    CASE
                    WHEN age_days <= 7  THEN 1
                    WHEN age_days <= 14 THEN 2
                    WHEN age_days <= 30 THEN 3
                    WHEN age_days <= 60 THEN 4
                    WHEN age_days <= 90 THEN 5
                    ELSE 6 END AS sort_key
                  FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                ) x
                GROUP BY bucket, sort_key
                ORDER BY sort_key
                """.formatted(KEPT),
                (rs, i) -> new AgeDistributionRow(rs.getString("bucket"), rs.getInt("sort_key"),
                        rs.getLong("n_postings"), rs.getDouble("pct")),
                rulesetId, snapshotDate);
    }

    public List<StaleByCompanyRow> staleByCompany(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT company, is_agency, COUNT(*) AS n_postings,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                       MAX(age_days) AS oldest_days,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 60 THEN 1 ELSE 0 END), 1) AS pct_over_60d
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                GROUP BY company, is_agency
                HAVING COUNT(*) >= 5
                ORDER BY avg_age_days DESC
                """.formatted(KEPT),
                (rs, i) -> new StaleByCompanyRow(rs.getString("company"), rs.getBoolean("is_agency"),
                        rs.getLong("n_postings"), dbl(rs, "avg_age_days"),
                        integer(rs, "oldest_days"), dbl(rs, "pct_over_60d")),
                rulesetId, snapshotDate);
    }

    public List<AgencyComparisonRow> agencyComparison(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT CASE WHEN is_agency THEN 'Recruitment agency' ELSE 'Direct employer' END AS poster_type,
                       COUNT(*) AS n_postings,
                       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 1) AS pct,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY age_days) AS median_age_days,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 60 THEN 1 ELSE 0 END), 1) AS pct_over_60d
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                GROUP BY is_agency
                """.formatted(KEPT),
                (rs, i) -> new AgencyComparisonRow(rs.getString("poster_type"), rs.getLong("n_postings"),
                        rs.getDouble("pct"), dbl(rs, "avg_age_days"),
                        dbl(rs, "median_age_days"), dbl(rs, "pct_over_60d")),
                rulesetId, snapshotDate);
    }

    public List<RoleBreakdownRow> roleBreakdown(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT country, role_group, role_family_key, seniority, COUNT(*) AS n_postings,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY age_days) AS median_age_days,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 7  THEN 1 ELSE 0 END), 1) AS pct_over_7d,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 30 THEN 1 ELSE 0 END), 1) AS pct_over_30d,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 60 THEN 1 ELSE 0 END), 1) AS pct_over_60d,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 90 THEN 1 ELSE 0 END), 1) AS pct_over_90d,
                       ROUND(100.0 * AVG(CASE WHEN language = 'en' THEN 1 ELSE 0 END), 1) AS pct_english
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s AND role_family_key <> 'other'
                GROUP BY country, role_group, role_family_key, seniority
                ORDER BY n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new RoleBreakdownRow(rs.getString("country"), rs.getString("role_group"),
                        rs.getString("role_family_key"), rs.getString("seniority"), rs.getLong("n_postings"),
                        dbl(rs, "avg_age_days"), dbl(rs, "median_age_days"),
                        dbl(rs, "pct_over_7d"), dbl(rs, "pct_over_30d"),
                        dbl(rs, "pct_over_60d"), dbl(rs, "pct_over_90d"),
                        dbl(rs, "pct_english")),
                rulesetId, snapshotDate);
    }

    public List<CityBreakdownRow> cityBreakdown(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT country, city, COUNT(*) AS n_postings,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                       ROUND(100.0 * AVG(CASE WHEN language = 'en' THEN 1 ELSE 0 END), 1) AS pct_english,
                       ROUND(100.0 * AVG(CASE WHEN is_agency THEN 1 ELSE 0 END), 1) AS pct_agency
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                GROUP BY country, city
                ORDER BY n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new CityBreakdownRow(rs.getString("country"), rs.getString("city"),
                        rs.getLong("n_postings"), dbl(rs, "avg_age_days"),
                        dbl(rs, "pct_english"), dbl(rs, "pct_agency")),
                rulesetId, snapshotDate);
    }

    public List<CityRoleBreakdownRow> cityRoleBreakdown(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT country, city, role_family_key, COUNT(*) AS n_postings,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                GROUP BY country, city, role_family_key
                HAVING COUNT(*) >= 1
                ORDER BY n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new CityRoleBreakdownRow(rs.getString("country"), rs.getString("city"),
                        rs.getString("role_family_key"), rs.getLong("n_postings"),
                        dbl(rs, "avg_age_days")),
                rulesetId, snapshotDate);
    }

    public List<SkillDemandRow> skillDemand(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT s.skill_key AS skill, MAX(s.skill_category) AS skill_category,
                       COUNT(DISTINCT s.posting_id) AS n_postings,
                       ROUND(100.0 * COUNT(DISTINCT s.posting_id) /
                             (SELECT COUNT(*) FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %1$s),
                             1) AS pct_postings,
                       ROUND(AVG(k.age_days)::numeric, 1) AS avg_age_days
                FROM posting_skill s
                JOIN posting k ON k.id = s.posting_id AND k.ruleset_id = ? AND k.snapshot_date = ? AND k.%1$s
                GROUP BY s.skill_key
                ORDER BY n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new SkillDemandRow(rs.getString("skill"), rs.getString("skill_category"),
                        rs.getLong("n_postings"), dbl(rs, "pct_postings"),
                        dbl(rs, "avg_age_days"), snapshotDate),
                rulesetId, snapshotDate, rulesetId, snapshotDate);
    }

    public List<SkillByRoleRow> skillByRole(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                WITH fam AS (
                  SELECT role_family_key, COUNT(*) AS n_fam
                  FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %1$s
                  GROUP BY role_family_key
                )
                SELECT k.role_family_key, s.skill_key AS skill, MAX(s.skill_category) AS skill_category,
                       COUNT(DISTINCT s.posting_id) AS n_postings,
                       MAX(f.n_fam) AS role_postings,
                       ROUND(100.0 * COUNT(DISTINCT s.posting_id) / MAX(f.n_fam), 1) AS pct_role_postings,
                       ROUND(AVG(k.age_days)::numeric, 1) AS avg_age_days
                FROM posting_skill s
                JOIN posting k ON k.id = s.posting_id AND k.ruleset_id = ? AND k.snapshot_date = ? AND k.%1$s
                JOIN fam f ON f.role_family_key = k.role_family_key
                WHERE f.n_fam >= 50
                GROUP BY k.role_family_key, s.skill_key
                HAVING COUNT(DISTINCT s.posting_id) >= 3
                ORDER BY k.role_family_key, n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new SkillByRoleRow(rs.getString("role_family_key"), rs.getString("skill"),
                        rs.getString("skill_category"), rs.getLong("n_postings"), rs.getLong("role_postings"),
                        dbl(rs, "pct_role_postings"), dbl(rs, "avg_age_days"),
                        snapshotDate),
                rulesetId, snapshotDate, rulesetId, snapshotDate);
    }

    public List<CountryBreakdownRow> countryBreakdown(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT country, COUNT(*) AS n_postings, COUNT(DISTINCT company) AS employers,
                       ROUND(AVG(age_days)::numeric, 1) AS avg_age_days,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY age_days) AS median_age_days,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 30 THEN 1 ELSE 0 END), 1) AS pct_over_30d,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 60 THEN 1 ELSE 0 END), 1) AS pct_over_60d,
                       ROUND(100.0 * AVG(CASE WHEN age_days > 90 THEN 1 ELSE 0 END), 1) AS pct_over_90d,
                       ROUND(100.0 * AVG(CASE WHEN language = 'en' THEN 1 ELSE 0 END), 1) AS pct_english,
                       ROUND(100.0 * AVG(CASE WHEN salary_min IS NOT NULL THEN 1 ELSE 0 END), 1) AS pct_with_salary,
                       ROUND(100.0 * AVG(CASE WHEN is_agency THEN 1 ELSE 0 END), 1) AS pct_agency
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                GROUP BY country
                ORDER BY n_postings DESC
                """.formatted(KEPT),
                (rs, i) -> new CountryBreakdownRow(rs.getString("country"), rs.getLong("n_postings"),
                        rs.getLong("employers"), dbl(rs, "avg_age_days"),
                        dbl(rs, "median_age_days"), dbl(rs, "pct_over_30d"),
                        dbl(rs, "pct_over_60d"), dbl(rs, "pct_over_90d"),
                        dbl(rs, "pct_english"), dbl(rs, "pct_with_salary"),
                        dbl(rs, "pct_agency")),
                rulesetId, snapshotDate);
    }

    public List<PostingPublicRow> postingsPublic(long rulesetId, LocalDate snapshotDate) {
        return jdbc.query("""
                SELECT posting_hash, title_raw, company, city, country, role_group, role_family_key,
                       seniority, language, is_agency, age_days, created_date, redirect_url
                FROM posting
                WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                  AND redirect_url IS NOT NULL AND length(redirect_url) > 0
                ORDER BY created_date DESC, posting_hash
                """.formatted(KEPT),
                (rs, i) -> new PostingPublicRow(rs.getString("posting_hash"), rs.getString("title_raw"),
                        rs.getString("company"), rs.getString("city"), rs.getString("country"),
                        rs.getString("role_group"), rs.getString("role_family_key"), rs.getString("seniority"),
                        rs.getString("language"), rs.getBoolean("is_agency"),
                        integer(rs, "age_days"),
                        rs.getObject("created_date", LocalDate.class), rs.getString("redirect_url"),
                        snapshotDate),
                rulesetId, snapshotDate);
    }

    public long quarantinedCount(long rulesetId, LocalDate snapshotDate) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND status = 'QUARANTINED'",
                Long.class, rulesetId, snapshotDate);
        return n == null ? 0 : n;
    }

    public Integer maxAgeDays(long rulesetId, LocalDate snapshotDate) {
        return jdbc.queryForObject(
                ("SELECT MAX(age_days) FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s")
                        .formatted(KEPT),
                Integer.class, rulesetId, snapshotDate);
    }

    public double descTruncatedPct(long rulesetId, LocalDate snapshotDate) {
        Double pct = jdbc.queryForObject(("""
                SELECT ROUND(100.0 * AVG(CASE WHEN desc_truncated THEN 1 ELSE 0 END), 1)
                FROM posting WHERE ruleset_id = ? AND snapshot_date = ? AND %s
                """).formatted(KEPT), Double.class, rulesetId, snapshotDate);
        return pct == null ? 0.0 : pct;
    }

    /** Deletes this snapshot's rows first so re-running export stays idempotent, then re-inserts. */
    public void recordHistory(long rulesetId, LocalDate snapshotDate, MarketSummary summary,
                               List<SkillDemandRow> skills, List<RoleBreakdownRow> roles) {
        historyMetricRepository.deleteByRulesetIdAndSnapshotDate(rulesetId, snapshotDate);

        record M(String metric, String dimension, Double value) {
        }
        java.util.List<M> rows = new java.util.ArrayList<>();
        rows.add(new M("live_postings", "all", (double) summary.livePostings()));
        rows.add(new M("avg_age_days", "all", summary.avgAgeDays()));
        rows.add(new M("pct_over_60d", "all", summary.pctOver60d()));
        skills.forEach(s -> rows.add(new M("skill_pct", s.skill(), s.pctPostings())));

        roles.stream()
                .collect(java.util.stream.Collectors.groupingBy(RoleBreakdownRow::roleFamily,
                        java.util.stream.Collectors.summingLong(RoleBreakdownRow::nPostings)))
                .forEach((family, count) -> rows.add(new M("role_count", family, (double) count)));

        rows.stream().filter(r -> r.value() != null).forEach(r -> {
            HistoryMetric hm = new HistoryMetric();
            hm.setRulesetId(rulesetId);
            hm.setSnapshotDate(snapshotDate);
            hm.setMetric(r.metric());
            hm.setDimension(r.dimension());
            hm.setValue(r.value());
            historyMetricRepository.save(hm);
        });
    }

    public List<HistoryRow> history(long rulesetId) {
        return historyMetricRepository.findByRulesetIdOrderBySnapshotDateAscMetricAscDimensionAsc(rulesetId).stream()
                .map(hm -> new HistoryRow(hm.getSnapshotDate(), hm.getMetric(), hm.getDimension(), hm.getValue()))
                .toList();
    }
}
