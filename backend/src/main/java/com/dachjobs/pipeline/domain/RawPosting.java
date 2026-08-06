package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_posting")
public class RawPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adzunaId;
    private String country;
    private String queryRole;
    private Integer queryPage;
    private LocalDate pullDate;
    private String title;
    private String company;
    private String cityRaw;
    private String area2;
    private String area3;
    private String description;
    private String redirectUrl;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Boolean salaryIsPredicted;
    private String contractType;
    private String contractTime;
    private String category;
    private LocalDateTime created;

    public Long getId() { return id; }
    public String getAdzunaId() { return adzunaId; }
    public String getCountry() { return country; }
    public String getQueryRole() { return queryRole; }
    public Integer getQueryPage() { return queryPage; }
    public LocalDate getPullDate() { return pullDate; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getCityRaw() { return cityRaw; }
    public String getArea2() { return area2; }
    public String getArea3() { return area3; }
    public String getDescription() { return description; }
    public String getRedirectUrl() { return redirectUrl; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public Boolean getSalaryIsPredicted() { return salaryIsPredicted; }
    public String getContractType() { return contractType; }
    public String getContractTime() { return contractTime; }
    public String getCategory() { return category; }
    public LocalDateTime getCreated() { return created; }

    public void setAdzunaId(String v) { this.adzunaId = v; }
    public void setCountry(String v) { this.country = v; }
    public void setQueryRole(String v) { this.queryRole = v; }
    public void setQueryPage(Integer v) { this.queryPage = v; }
    public void setPullDate(LocalDate v) { this.pullDate = v; }
    public void setTitle(String v) { this.title = v; }
    public void setCompany(String v) { this.company = v; }
    public void setCityRaw(String v) { this.cityRaw = v; }
    public void setArea2(String v) { this.area2 = v; }
    public void setArea3(String v) { this.area3 = v; }
    public void setDescription(String v) { this.description = v; }
    public void setRedirectUrl(String v) { this.redirectUrl = v; }
    public void setSalaryMin(BigDecimal v) { this.salaryMin = v; }
    public void setSalaryMax(BigDecimal v) { this.salaryMax = v; }
    public void setSalaryIsPredicted(Boolean v) { this.salaryIsPredicted = v; }
    public void setContractType(String v) { this.contractType = v; }
    public void setContractTime(String v) { this.contractTime = v; }
    public void setCategory(String v) { this.category = v; }
    public void setCreated(LocalDateTime v) { this.created = v; }
}
