package com.dachjobs.pipeline.classify;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ports the city/region derivation in notebooks/03_silver_clean.py: Adzuna's
 * location.area is a coarse-to-fine list (country, state/canton, district...),
 * so area[1] (region) and area[2] (district) are the useful levels, with a
 * short list of city-states where the region IS the city.
 */
public final class CityNormalizer {

    private static final Set<String> CITY_STATES = Set.of(
            "berlin", "hamburg", "bremen", "wien",
            "basel-stadt", "genf", "geneve", "geneva");

    private static final Pattern TRAILING_PARENTHETICAL = Pattern.compile("\\s*\\(.*?\\)\\s*$");
    private static final Pattern TRAILING_AREA_SUFFIX =
            Pattern.compile("[-\\s](umgebung|umland|land|stadt)$");
    private static final Pattern LEADING_REGION_PREFIX =
            Pattern.compile("^(region|regionalverband)\\s+");
    private static final Pattern BREISGAU_OR_MITTELLAND = Pattern.compile("(\\s+im\\s+breisgau|-mittelland)$");
    private static final Pattern AM_MAIN = Pattern.compile(" am main$");

    private CityNormalizer() {
    }

    public record Result(String city, String region) {
    }

    /** area2/area3 are Adzuna's location.area[1]/[2], raw (not yet folded). */
    public static Result normalize(String area2, String area3) {
        String foldedArea2 = area2 == null ? null : TitleNormalizer.normalize(area2);
        String foldedArea3 = area3 == null ? null : TitleNormalizer.normalize(area3);

        String region = foldedArea2 != null ? foldedArea2 : "unknown";

        String city;
        if (foldedArea2 != null && CITY_STATES.contains(foldedArea2)) {
            city = foldedArea2;
        } else if (foldedArea3 != null) {
            city = foldedArea3;
        } else {
            city = foldedArea2 != null ? foldedArea2 : "unknown";
        }

        city = TRAILING_PARENTHETICAL.matcher(city).replaceAll("").trim();
        city = TRAILING_AREA_SUFFIX.matcher(city).replaceAll("");
        city = LEADING_REGION_PREFIX.matcher(city).replaceAll("");
        city = BREISGAU_OR_MITTELLAND.matcher(city).replaceAll("");
        city = AM_MAIN.matcher(city).replaceAll("");
        city = city.trim();

        return new Result(city, region);
    }
}
