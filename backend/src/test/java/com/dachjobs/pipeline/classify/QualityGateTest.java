package com.dachjobs.pipeline.classify;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Ports the RULES dict in notebooks/03_silver_clean.py one rule at a time. */
class QualityGateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

    private static PostingDraft valid() {
        return new PostingDraft(
                1L, "hash", "adzuna-1", "de",
                "Data Engineer", "data engineer", "data engineer",
                "mid", false,
                "Acme GmbH", "acme gmbh", false,
                "berlin", "Berlin", "berlin", "IT", "en",
                "https://example.com/job/1", "We build pipelines.", 20, false,
                new BigDecimal("60000"), new BigDecimal("80000"), false,
                "permanent", "full_time",
                LocalDate.of(2026, 7, 1), 35, TODAY, "data engineer");
    }

    @Test
    void aWellFormedDraftPassesEveryRule() {
        assertThat(QualityGate.failedRules(valid(), TODAY)).isEmpty();
    }

    @Test
    void tooShortATitleFailsTitlePresent() {
        PostingDraft d = withTitle(valid(), "Dev");
        assertThat(QualityGate.failedRules(d, TODAY)).contains("title_present");
    }

    @Test
    void invalidFamilyFailsNotInvalid() {
        PostingDraft d = withFamily(valid(), "invalid");
        assertThat(QualityGate.failedRules(d, TODAY)).contains("not_invalid");
    }

    @Test
    void missingCompanyFailsCompanyPresent() {
        PostingDraft d = withCompany(valid(), null);
        assertThat(QualityGate.failedRules(d, TODAY)).contains("company_present");
    }

    @Test
    void futureCreatedDateFailsDatePlausible() {
        PostingDraft d = withCreatedDate(valid(), TODAY.plusDays(1));
        assertThat(QualityGate.failedRules(d, TODAY)).contains("date_plausible");
    }

    @Test
    void createdBefore2024FailsDatePlausible() {
        PostingDraft d = withCreatedDate(valid(), LocalDate.of(2023, 12, 31));
        assertThat(QualityGate.failedRules(d, TODAY)).contains("date_plausible");
    }

    @Test
    void negativeAgeFailsAgeNonNegative() {
        PostingDraft base = valid();
        PostingDraft d = new PostingDraft(base.rawPostingId(), base.postingHash(), base.adzunaId(),
                base.country(), base.titleRaw(), base.titleNorm(), base.roleFamilyKey(), base.seniority(),
                base.genderedTag(), base.company(), base.companyNorm(), base.isAgency(), base.city(),
                base.cityRaw(), base.region(), base.category(), base.language(), base.redirectUrl(),
                base.description(), base.descChars(), base.descTruncated(), base.salaryMin(), base.salaryMax(),
                base.salaryIsPredicted(), base.contractType(), base.contractTime(), base.createdDate(), -1,
                base.snapshotDate(), base.queryRole());
        assertThat(QualityGate.failedRules(d, TODAY)).contains("age_non_negative");
    }

    @Test
    void salaryBelowFloorFailsSalarySane() {
        PostingDraft d = withSalary(valid(), new BigDecimal("5000"), new BigDecimal("80000"));
        assertThat(QualityGate.failedRules(d, TODAY)).contains("salary_sane");
    }

    @Test
    void salaryAboveCeilingFailsSalarySane() {
        PostingDraft d = withSalary(valid(), new BigDecimal("500000"), new BigDecimal("600000"));
        assertThat(QualityGate.failedRules(d, TODAY)).contains("salary_sane");
    }

    @Test
    void maxBelowMinFailsSalaryOrdered() {
        PostingDraft d = withSalary(valid(), new BigDecimal("80000"), new BigDecimal("60000"));
        assertThat(QualityGate.failedRules(d, TODAY)).contains("salary_ordered");
    }

    @Test
    void nullSalariesAreFine() {
        PostingDraft d = withSalary(valid(), null, null);
        assertThat(QualityGate.failedRules(d, TODAY)).isEmpty();
    }

    private static PostingDraft withTitle(PostingDraft b, String title) {
        return replace(b, title, b.company(), b.roleFamilyKey(), b.createdDate(), b.ageDays(),
                b.salaryMin(), b.salaryMax());
    }

    private static PostingDraft withFamily(PostingDraft b, String family) {
        return replace(b, b.titleRaw(), b.company(), family, b.createdDate(), b.ageDays(),
                b.salaryMin(), b.salaryMax());
    }

    private static PostingDraft withCompany(PostingDraft b, String company) {
        return replace(b, b.titleRaw(), company, b.roleFamilyKey(), b.createdDate(), b.ageDays(),
                b.salaryMin(), b.salaryMax());
    }

    private static PostingDraft withCreatedDate(PostingDraft b, LocalDate createdDate) {
        return replace(b, b.titleRaw(), b.company(), b.roleFamilyKey(), createdDate, b.ageDays(),
                b.salaryMin(), b.salaryMax());
    }

    private static PostingDraft withSalary(PostingDraft b, BigDecimal min, BigDecimal max) {
        return replace(b, b.titleRaw(), b.company(), b.roleFamilyKey(), b.createdDate(), b.ageDays(), min, max);
    }

    private static PostingDraft replace(PostingDraft b, String titleRaw, String company, String roleFamilyKey,
                                         LocalDate createdDate, Integer ageDays, BigDecimal salaryMin,
                                         BigDecimal salaryMax) {
        return new PostingDraft(b.rawPostingId(), b.postingHash(), b.adzunaId(), b.country(), titleRaw,
                b.titleNorm(), roleFamilyKey, b.seniority(), b.genderedTag(), company, b.companyNorm(),
                b.isAgency(), b.city(), b.cityRaw(), b.region(), b.category(), b.language(), b.redirectUrl(),
                b.description(), b.descChars(), b.descTruncated(), salaryMin, salaryMax, b.salaryIsPredicted(),
                b.contractType(), b.contractTime(), createdDate, ageDays, b.snapshotDate(), b.queryRole());
    }
}
