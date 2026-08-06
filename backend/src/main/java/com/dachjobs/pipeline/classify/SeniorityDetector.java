package com.dachjobs.pipeline.classify;

import java.util.regex.Pattern;

/** Ports the seniority F.when() chain in notebooks/03_silver_clean.py. */
public final class SeniorityDetector {

    private static final Pattern SENIOR = Pattern.compile(
            "\\b(senior|sr\\.?|lead|principal|head|director|staff|chief|\\bvp\\b)\\b");
    private static final Pattern JUNIOR = Pattern.compile(
            "\\b(junior|jr\\.?|entry|graduate|absolvent|einsteiger|associate)\\b");

    private SeniorityDetector() {
    }

    /** titleNorm must already be folded, e.g. via {@link TitleNormalizer#normalize}. */
    public static String detect(String titleNorm) {
        if (SENIOR.matcher(titleNorm).find()) {
            return "senior";
        }
        if (JUNIOR.matcher(titleNorm).find()) {
            return "junior";
        }
        return "mid";
    }
}
