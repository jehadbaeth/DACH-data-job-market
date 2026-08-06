package com.dachjobs.pipeline.classify;

import java.util.regex.Pattern;

/**
 * Ports the language F.when() in notebooks/03_silver_clean.py. Deliberately
 * case-sensitive (matches Spark's default rlike behaviour) - the capitalised
 * German words are the signal, not the lowercase connectors alone.
 */
public final class LanguageDetector {

    private static final Pattern GERMAN_MARKERS = Pattern.compile(
            "\\b(und|der|die|das|mit|f\u00fcr|Kenntnisse|Erfahrung|Wir suchen|Deine|Ihre)\\b");

    private LanguageDetector() {
    }

    /** Raw (non-folded) description text. */
    public static String detect(String description) {
        if (description != null && GERMAN_MARKERS.matcher(description).find()) {
            return "de";
        }
        return "en";
    }
}
