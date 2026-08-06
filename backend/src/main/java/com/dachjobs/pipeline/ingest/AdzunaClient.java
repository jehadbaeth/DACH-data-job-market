package com.dachjobs.pipeline.ingest;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Direct port of the fetch() function in notebooks/01_ingest_adzuna.py:
 * up to 5 attempts, exponential backoff on HTTP 429, page-past-the-end
 * (HTTP 410) treated as an empty page rather than an error.
 */
@Component
public class AdzunaClient {

    private final RestClient restClient;
    private final AdzunaProperties properties;

    public AdzunaClient(AdzunaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public AdzunaSearchResponse fetch(String country, String role, int page) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/{country}/search/{page}")
                                .queryParam("app_id", properties.getAppId())
                                .queryParam("app_key", properties.getAppKey())
                                .queryParam("results_per_page", properties.getResultsPerPage())
                                .queryParam("what", role)
                                .queryParam("content-type", "application/json")
                                .build(country, page))
                        .retrieve()
                        .body(AdzunaSearchResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                sleep((long) Math.pow(2, attempt) * 1000);
            } catch (HttpClientErrorException.Gone e) {
                AdzunaSearchResponse empty = new AdzunaSearchResponse();
                empty.setResults(java.util.List.of());
                empty.setCount(0);
                return empty;
            }
        }
        throw new IllegalStateException("gave up on " + country + "/" + role + "/p" + page + " after 5 attempts");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
