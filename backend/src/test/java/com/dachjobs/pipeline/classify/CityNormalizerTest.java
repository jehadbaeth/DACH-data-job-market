package com.dachjobs.pipeline.classify;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Same cleanup cases the comments in notebooks/03_silver_clean.py call out by name. */
class CityNormalizerTest {

    static Stream<Arguments> cases() {
        return Stream.of(
                // city-state: the region IS the city
                Arguments.of("Berlin", "Kreuzberg", "berlin", "berlin"),
                Arguments.of("Wien", "Wien Umgebung", "wien", "wien"),
                // ordinary region: district (area3) wins over the region name
                Arguments.of("Bayern", "Muenchen (Kreis)", "bayern", "muenchen"),
                Arguments.of("Steiermark", "Graz-Umgebung", "steiermark", "graz"),
                Arguments.of("Oberoesterreich", "Linz-Land", "oberoesterreich", "linz"),
                Arguments.of("Niedersachsen", "Region Hannover", "niedersachsen", "hannover"),
                Arguments.of("Baden-Wuerttemberg", "Freiburg im Breisgau", "baden-wuerttemberg", "freiburg"),
                Arguments.of("Bern", "Bern-Mittelland", "bern", "bern"),
                Arguments.of("Hessen", "Frankfurt am Main", "hessen", "frankfurt"),
                // no district at all: falls back to the region
                Arguments.of("Sachsen", null, "sachsen", "sachsen"),
                // nothing at all
                Arguments.of(null, null, "unknown", "unknown")
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void normalizesLikeTheOriginalPipeline(String area2, String area3, String expectedRegion, String expectedCity) {
        CityNormalizer.Result result = CityNormalizer.normalize(area2, area3);
        assertThat(result.region()).isEqualTo(expectedRegion);
        assertThat(result.city()).isEqualTo(expectedCity);
    }
}
