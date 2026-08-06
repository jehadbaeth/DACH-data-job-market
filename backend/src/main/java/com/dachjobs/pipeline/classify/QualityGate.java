package com.dachjobs.pipeline.classify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ports the RULES dict and quarantine step in notebooks/03_silver_clean.py.
 * A draft with any failed rule is quarantined rather than published, same
 * as the original {@code passed}/{@code quarantine} split.
 */
public final class QualityGate {

    private static final LocalDate EARLIEST_PLAUSIBLE_DATE = LocalDate.of(2024, 1, 1);
    private static final BigDecimal MIN_SANE_SALARY = BigDecimal.valueOf(12000);
    private static final BigDecimal MAX_SANE_SALARY = BigDecimal.valueOf(400000);

    private QualityGate() {
    }

    /** Names of every rule the draft fails; empty means it passes the gate. */
    public static List<String> failedRules(PostingDraft d, LocalDate today) {
        List<String> failed = new ArrayList<>();

        if (!(d.titleRaw() != null && d.titleRaw().length() > 3)) {
            failed.add("title_present");
        }
        if ("invalid".equals(d.roleFamilyKey())) {
            failed.add("not_invalid");
        }
        if (!(d.company() != null && d.company().length() > 1)) {
            failed.add("company_present");
        }
        if (d.city() == null) {
            failed.add("city_present");
        }
        if (!(d.createdDate() != null
                && !d.createdDate().isBefore(EARLIEST_PLAUSIBLE_DATE)
                && !d.createdDate().isAfter(today))) {
            failed.add("date_plausible");
        }
        if (!(d.ageDays() != null && d.ageDays() >= 0)) {
            failed.add("age_non_negative");
        }
        if (!(d.salaryMin() == null
                || (d.salaryMin().compareTo(MIN_SANE_SALARY) >= 0
                    && d.salaryMin().compareTo(MAX_SANE_SALARY) <= 0))) {
            failed.add("salary_sane");
        }
        if (!(d.salaryMax() == null || d.salaryMin() == null
                || d.salaryMax().compareTo(d.salaryMin()) >= 0)) {
            failed.add("salary_ordered");
        }

        return failed;
    }
}
