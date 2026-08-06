package com.dachjobs.pipeline.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/** Shape of a single page of https://api.adzuna.com/v1/api/jobs/{country}/search/{page}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdzunaSearchResponse {

    private int count;
    private List<Result> results = List.of();

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Result {
        private String id;
        private String title;
        private String description;
        private String redirectUrl;
        private java.math.BigDecimal salaryMin;
        private java.math.BigDecimal salaryMax;
        // Adzuna sends this as the string "0"/"1", not a JSON boolean.
        private String salaryIsPredicted;
        private String contractType;
        private String contractTime;
        private String created;
        private Company company;
        private Location location;
        private Category category;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
        public java.math.BigDecimal getSalaryMin() { return salaryMin; }
        public void setSalaryMin(java.math.BigDecimal salaryMin) { this.salaryMin = salaryMin; }
        public java.math.BigDecimal getSalaryMax() { return salaryMax; }
        public void setSalaryMax(java.math.BigDecimal salaryMax) { this.salaryMax = salaryMax; }
        public String getSalaryIsPredicted() { return salaryIsPredicted; }
        public void setSalaryIsPredicted(String salaryIsPredicted) { this.salaryIsPredicted = salaryIsPredicted; }
        public String getContractType() { return contractType; }
        public void setContractType(String contractType) { this.contractType = contractType; }
        public String getContractTime() { return contractTime; }
        public void setContractTime(String contractTime) { this.contractTime = contractTime; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public Company getCompany() { return company; }
        public void setCompany(Company company) { this.company = company; }
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        public Category getCategory() { return category; }
        public void setCategory(Category category) { this.category = category; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Company {
        private String displayName;
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Location {
        private String displayName;
        private List<String> area = List.of();
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public List<String> getArea() { return area; }
        public void setArea(List<String> area) { this.area = area; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Category {
        private String label;
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
