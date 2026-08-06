package com.dachjobs.pipeline.ingest;

import com.dachjobs.pipeline.domain.RawPosting;
import com.dachjobs.pipeline.repo.RawPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ports the loop-termination behaviour of the fetch loop in
 * notebooks/01_ingest_adzuna.py: stop a role's pages once a page returns
 * fewer than results-per-page results, and never re-save a posting already
 * pulled the same day (the DB equivalent of the original file-based cache).
 */
@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    @Mock
    private AdzunaClient client;
    @Mock
    private RawPostingRepository rawPostingRepository;

    private final AdzunaProperties properties = properties();

    private static AdzunaProperties properties() {
        AdzunaProperties p = new AdzunaProperties();
        p.setBaseUrl("https://api.adzuna.com/v1/api/jobs");
        p.setAppId("id");
        p.setAppKey("key");
        p.setCountries(List.of("de"));
        p.setRoles(List.of("data engineer"));
        p.setMaxPages(45);
        p.setResultsPerPage(50);
        return p;
    }

    private static AdzunaSearchResponse pageOf(int n) {
        AdzunaSearchResponse response = new AdzunaSearchResponse();
        response.setCount(n);
        List<AdzunaSearchResponse.Result> results = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            AdzunaSearchResponse.Result r = new AdzunaSearchResponse.Result();
            r.setId("id-" + i);
            r.setTitle("Data Engineer");
            results.add(r);
        }
        response.setResults(results);
        return response;
    }

    @Test
    void stopsPaginatingOnceAPageIsShortOfAFullPage() {
        when(client.fetch(eq("de"), eq("data engineer"), eq(1))).thenReturn(pageOf(50));
        when(client.fetch(eq("de"), eq("data engineer"), eq(2))).thenReturn(pageOf(50));
        when(client.fetch(eq("de"), eq("data engineer"), eq(3))).thenReturn(pageOf(12));

        IngestService service = new IngestService(client, properties, rawPostingRepository);
        IngestService.IngestSummary summary = service.run();

        verify(client, never()).fetch(eq("de"), eq("data engineer"), eq(4));
        assertThat(summary.callsMade()).isEqualTo(3);
        assertThat(summary.postingsSaved()).isEqualTo(112);
    }

    @Test
    void skipsPostingsAlreadyPulledToday() {
        when(client.fetch(any(), any(), eq(1))).thenReturn(pageOf(3));
        when(rawPostingRepository.existsByAdzunaIdAndPullDate(eq("id-0"), any(LocalDate.class))).thenReturn(true);

        IngestService service = new IngestService(client, properties, rawPostingRepository);
        IngestService.IngestSummary summary = service.run();

        assertThat(summary.postingsSaved()).isEqualTo(2);
        assertThat(summary.postingsSkippedAlreadyPulledToday()).isEqualTo(1);
        verify(rawPostingRepository, never()).save(argThat(rp -> rp.getAdzunaId().equals("id-0")));
    }

    @Test
    void mapsAreaAndCompanyFieldsOntoTheRawPostingEntity() {
        AdzunaSearchResponse.Result r = new AdzunaSearchResponse.Result();
        r.setId("abc");
        r.setTitle("Data Engineer");
        AdzunaSearchResponse.Company company = new AdzunaSearchResponse.Company();
        company.setDisplayName("Acme GmbH");
        r.setCompany(company);
        AdzunaSearchResponse.Location location = new AdzunaSearchResponse.Location();
        location.setDisplayName("Berlin");
        location.setArea(List.of("Germany", "Berlin", "Mitte"));
        r.setLocation(location);

        AdzunaSearchResponse page = new AdzunaSearchResponse();
        page.setCount(1);
        page.setResults(List.of(r));

        when(client.fetch(any(), any(), eq(1))).thenReturn(page);

        IngestService service = new IngestService(client, properties, rawPostingRepository);
        service.run();

        ArgumentCaptor<RawPosting> captor = ArgumentCaptor.forClass(RawPosting.class);
        verify(rawPostingRepository).save(captor.capture());
        RawPosting saved = captor.getValue();
        assertThat(saved.getCompany()).isEqualTo("Acme GmbH");
        assertThat(saved.getCityRaw()).isEqualTo("Berlin");
        assertThat(saved.getArea2()).isEqualTo("Berlin");
        assertThat(saved.getArea3()).isEqualTo("Mitte");
    }
}
