package com.dachjobs.pipeline.classify;

/**
 * Ports fold() from notebooks/03_silver_clean.py: lowercase, trim, and fold
 * umlauts so grouping/regex matching behaves the same for "Datenanalyst"
 * regardless of how the source spelled its umlauts.
 */
public final class TitleNormalizer {

    private TitleNormalizer() {
    }

    public static String normalize(String title) {
        if (title == null) {
            return "";
        }
        String s = title.toLowerCase().trim();
        s = s.replace("ä", "ae")
             .replace("ö", "oe")
             .replace("ü", "ue")
             .replace("ß", "ss");
        return s;
    }
}
