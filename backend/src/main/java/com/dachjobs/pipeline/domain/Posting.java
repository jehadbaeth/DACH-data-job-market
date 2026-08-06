package com.dachjobs.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "posting")
public class Posting {

    public enum Status { KEPT, EXCLUDED, QUARANTINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;
    private String postingHash;
    private String dedupHash;
    private Long rawPostingId;
    private String adzunaId;
    private String country;
    private String titleRaw;
    private String titleNorm;
    private String roleFamilyKey;
    private String roleGroup;
    private String seniority;
    private boolean genderedTag;
    private String company;
    private String companyNorm;
    private boolean isAgency;
    private String city;
    private String cityRaw;
    private String region;
    private String category;
    private String language;
    private String redirectUrl;
    private String description;
    private Integer descChars;
    private Boolean descTruncated;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Boolean salaryIsPredicted;
    private String contractType;
    private String contractTime;
    private LocalDate createdDate;
    private Integer ageDays;
    private LocalDate snapshotDate;
    private String queryRole;
    private String status;
    private String exclusionReason;

    public Long getId() { return id; }
    public Long getRulesetId() { return rulesetId; }
    public String getPostingHash() { return postingHash; }
    public String getDedupHash() { return dedupHash; }
    public Long getRawPostingId() { return rawPostingId; }
    public String getAdzunaId() { return adzunaId; }
    public String getCountry() { return country; }
    public String getTitleRaw() { return titleRaw; }
    public String getTitleNorm() { return titleNorm; }
    public String getRoleFamilyKey() { return roleFamilyKey; }
    public String getRoleGroup() { return roleGroup; }
    public String getSeniority() { return seniority; }
    public boolean isGenderedTag() { return genderedTag; }
    public String getCompany() { return company; }
    public String getCompanyNorm() { return companyNorm; }
    public boolean isAgency() { return isAgency; }
    public String getCity() { return city; }
    public String getCityRaw() { return cityRaw; }
    public String getRegion() { return region; }
    public String getCategory() { return category; }
    public String getLanguage() { return language; }
    public String getRedirectUrl() { return redirectUrl; }
    public String getDescription() { return description; }
    public Integer getDescChars() { return descChars; }
    public Boolean getDescTruncated() { return descTruncated; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public Boolean getSalaryIsPredicted() { return salaryIsPredicted; }
    public String getContractType() { return contractType; }
    public String getContractTime() { return contractTime; }
    public LocalDate getCreatedDate() { return createdDate; }
    public Integer getAgeDays() { return ageDays; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public String getQueryRole() { return queryRole; }
    public String getStatus() { return status; }
    public String getExclusionReason() { return exclusionReason; }

    public void setRulesetId(Long v) { this.rulesetId = v; }
    public void setPostingHash(String v) { this.postingHash = v; }
    public void setDedupHash(String v) { this.dedupHash = v; }
    public void setRawPostingId(Long v) { this.rawPostingId = v; }
    public void setAdzunaId(String v) { this.adzunaId = v; }
    public void setCountry(String v) { this.country = v; }
    public void setTitleRaw(String v) { this.titleRaw = v; }
    public void setTitleNorm(String v) { this.titleNorm = v; }
    public void setRoleFamilyKey(String v) { this.roleFamilyKey = v; }
    public void setRoleGroup(String v) { this.roleGroup = v; }
    public void setSeniority(String v) { this.seniority = v; }
    public void setGenderedTag(boolean v) { this.genderedTag = v; }
    public void setCompany(String v) { this.company = v; }
    public void setCompanyNorm(String v) { this.companyNorm = v; }
    public void setAgency(boolean v) { this.isAgency = v; }
    public void setCity(String v) { this.city = v; }
    public void setCityRaw(String v) { this.cityRaw = v; }
    public void setRegion(String v) { this.region = v; }
    public void setCategory(String v) { this.category = v; }
    public void setLanguage(String v) { this.language = v; }
    public void setRedirectUrl(String v) { this.redirectUrl = v; }
    public void setDescription(String v) { this.description = v; }
    public void setDescChars(Integer v) { this.descChars = v; }
    public void setDescTruncated(Boolean v) { this.descTruncated = v; }
    public void setSalaryMin(BigDecimal v) { this.salaryMin = v; }
    public void setSalaryMax(BigDecimal v) { this.salaryMax = v; }
    public void setSalaryIsPredicted(Boolean v) { this.salaryIsPredicted = v; }
    public void setContractType(String v) { this.contractType = v; }
    public void setContractTime(String v) { this.contractTime = v; }
    public void setCreatedDate(LocalDate v) { this.createdDate = v; }
    public void setAgeDays(Integer v) { this.ageDays = v; }
    public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public void setQueryRole(String v) { this.queryRole = v; }
    public void setStatus(String v) { this.status = v; }
    public void setExclusionReason(String v) { this.exclusionReason = v; }
}
