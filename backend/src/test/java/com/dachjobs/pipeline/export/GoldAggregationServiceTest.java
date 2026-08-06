package com.dachjobs.pipeline.export;

import com.dachjobs.pipeline.export.GoldExportRecords.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the actual SQL in GoldAggregationService against a real (H2,
 * PostgreSQL-compatibility-mode) database with the real Flyway schema,
 * so PERCENTILE_CONT/::numeric/window-function syntax is exercised, not
 * just mocked away like the classification tests.
 */
@SpringBootTest(classes = com.dachjobs.pipeline.DachJobsApplication.class)
@Transactional
class GoldAggregationServiceTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private GoldAggregationService service;

    private long rulesetId;
    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 8, 5);

    @BeforeEach
    void seed() {
        SimpleJdbcInsert rulesetInsert = new SimpleJdbcInsert(jdbc).withTableName("ruleset").usingGeneratedKeyColumns("id");
        Map<String, Object> rs = new HashMap<>();
        rs.put("key", "ai-" + System.nanoTime());
        rs.put("label", "AI test ruleset");
        rulesetId = rulesetInsert.executeAndReturnKey(rs).longValue();

        long rawId = insertRaw();
        long p1 = insertPosting(rawId, "hash1", "a1", "de", "berlin", "data engineer", "data", "mid", false,
                45, 60000, false, "en");
        long p2 = insertPosting(rawId, "hash2", "a2", "de", "berlin", "data engineer", "data", "senior", false,
                10, null, false, "de");
        long p3 = insertPosting(rawId, "hash3", "a3", "at", "wien", "ai engineer", "ai", "junior", true,
                70, null, false, "en");

        insertSkill(p1, "python", "language");
        insertSkill(p1, "spark", "framework");
        insertSkill(p2, "python", "language");
        insertSkill(p3, "python", "language");
    }

    private long insertRaw() {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbc).withTableName("raw_posting").usingGeneratedKeyColumns("id");
        Map<String, Object> r = new HashMap<>();
        r.put("adzuna_id", "seed");
        r.put("country", "de");
        r.put("query_role", "data engineer");
        r.put("pull_date", SNAPSHOT);
        r.put("ingest_ts", java.time.OffsetDateTime.now());
        return insert.executeAndReturnKey(r).longValue();
    }

    private long insertPosting(long rawId, String hash, String adzunaId, String country, String city,
                                String family, String group, String seniority, boolean agency,
                                int ageDays, Integer salaryMin, boolean truncated, String language) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbc).withTableName("posting").usingGeneratedKeyColumns("id");
        Map<String, Object> p = new HashMap<>();
        p.put("ruleset_id", rulesetId);
        p.put("posting_hash", hash);
        p.put("raw_posting_id", rawId);
        p.put("adzuna_id", adzunaId);
        p.put("country", country);
        p.put("title_raw", family);
        p.put("title_norm", family);
        p.put("role_family_key", family);
        p.put("role_group", group);
        p.put("seniority", seniority);
        p.put("gendered_tag", false);
        p.put("company", "Acme " + adzunaId);
        p.put("company_norm", "acme " + adzunaId);
        p.put("is_agency", agency);
        p.put("city", city);
        p.put("city_raw", city);
        p.put("region", country);
        p.put("language", language);
        p.put("redirect_url", "https://example.com/" + adzunaId);
        p.put("desc_truncated", truncated);
        p.put("salary_min", salaryMin == null ? null : BigDecimal.valueOf(salaryMin));
        p.put("created_date", SNAPSHOT.minusDays(ageDays));
        p.put("age_days", ageDays);
        p.put("snapshot_date", SNAPSHOT);
        p.put("status", "KEPT");
        p.put("ingest_ts", java.time.OffsetDateTime.now());
        return insert.executeAndReturnKey(p).longValue();
    }

    private void insertSkill(long postingId, String key, String category) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbc).withTableName("posting_skill").usingGeneratedKeyColumns("id");
        Map<String, Object> s = new HashMap<>();
        s.put("posting_id", postingId);
        s.put("skill_key", key);
        s.put("skill_category", category);
        insert.execute(s);
    }

    @Test
    void marketSummaryCountsAllKeptPostings() {
        MarketSummary summary = service.marketSummary(rulesetId, SNAPSHOT);
        assertThat(summary.livePostings()).isEqualTo(3);
        assertThat(summary.employers()).isEqualTo(3);
        assertThat(summary.pctAgency()).isEqualTo(33.3);
        assertThat(summary.pctEnglish()).isCloseTo(66.7, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void ageDistributionBucketsByAgeDays() {
        List<AgeDistributionRow> rows = service.ageDistribution(rulesetId, SNAPSHOT);
        assertThat(rows).extracting(AgeDistributionRow::bucket).contains("8-14 days", "31-60 days", "61-90 days");
    }

    @Test
    void roleBreakdownExcludesOtherAndGroupsBySeniority() {
        List<RoleBreakdownRow> rows = service.roleBreakdown(rulesetId, SNAPSHOT);
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(RoleBreakdownRow::roleFamily).contains("data engineer", "ai engineer");
    }

    @Test
    void skillDemandCountsDistinctPostingsPerSkill() {
        List<SkillDemandRow> rows = service.skillDemand(rulesetId, SNAPSHOT);
        Map<String, Long> bySkill = new HashMap<>();
        rows.forEach(r -> bySkill.put(r.skill(), r.nPostings()));
        assertThat(bySkill.get("python")).isEqualTo(3L);
        assertThat(bySkill.get("spark")).isEqualTo(1L);
    }

    @Test
    void postingsPublicOnlyIncludesRowsWithARedirectUrl() {
        List<PostingPublicRow> rows = service.postingsPublic(rulesetId, SNAPSHOT);
        assertThat(rows).hasSize(3);
        assertThat(rows).allMatch(r -> r.url() != null && !r.url().isEmpty());
    }

    @Test
    void quarantinedCountIsZeroWhenNoneWereInserted() {
        assertThat(service.quarantinedCount(rulesetId, SNAPSHOT)).isZero();
    }
}
