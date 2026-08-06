package com.dachjobs.pipeline.classify;

import java.util.regex.Pattern;

/** Ports the gendered_tag F.rlike() in notebooks/03_silver_clean.py, e.g. "(m/w/d)". */
public final class GenderedTagDetector {

    private static final Pattern GENDERED_TAG = Pattern.compile("\\(?\\s*[mwdfxa](\\s*/\\s*[mwdfxa]){1,3}");

    private GenderedTagDetector() {
    }

    /** titleNorm must already be folded, e.g. via {@link TitleNormalizer#normalize}. */
    public static boolean hasTag(String titleNorm) {
        return GENDERED_TAG.matcher(titleNorm).find();
    }
}
