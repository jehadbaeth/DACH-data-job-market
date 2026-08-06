package com.dachjobs.pipeline.classify;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Every computed field for one raw posting, before the quality gate and
 * dedup decide whether/how it becomes a {@code posting} row. Deliberately
 * plain so {@link QualityGate} can be unit tested without a database.
 */
public record PostingDraft(
        Long rawPostingId,
        String postingHash,
        String adzunaId,
        String country,
        String titleRaw,
        String titleNorm,
        String roleFamilyKey,
        String seniority,
        boolean genderedTag,
        String company,
        String companyNorm,
        boolean isAgency,
        String city,
        String cityRaw,
        String region,
        String category,
        String language,
        String redirectUrl,
        String description,
        Integer descChars,
        Boolean descTruncated,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Boolean salaryIsPredicted,
        String contractType,
        String contractTime,
        LocalDate createdDate,
        Integer ageDays,
        LocalDate snapshotDate,
        String queryRole) {
}
