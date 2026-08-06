package com.dachjobs.pipeline.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "dachjobs.adzuna")
public class AdzunaProperties {

    private String baseUrl;
    private String appId;
    private String appKey;
    private List<String> countries;
    private List<String> roles;
    private int maxPages;
    private int resultsPerPage;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }

    public List<String> getCountries() { return countries; }
    public void setCountries(List<String> countries) { this.countries = countries; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }

    public int getResultsPerPage() { return resultsPerPage; }
    public void setResultsPerPage(int resultsPerPage) { this.resultsPerPage = resultsPerPage; }
}
