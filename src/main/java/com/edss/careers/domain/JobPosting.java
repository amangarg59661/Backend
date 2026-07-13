package com.edss.careers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Job posting aggregate. Lifecycle: draft → published → archived. Only
 * `published` postings are visible to the public GET /api/v1/careers
 * surface. Editing is always allowed while in draft; edits after publish
 * are allowed but recorded (see updated_at).
 */
@Entity
@Table(schema = "careers", name = "job_postings")
public class JobPosting {

    @Id private UUID id;

    private String slug;

    private String title;

    private String team;

    private String location;

    @Column(name = "employment_type")
    private String employmentType;

    private String commitment;

    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> responsibilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> requirements;

    @Column(name = "salary_range_min")
    private Long salaryRangeMin;

    @Column(name = "salary_range_max")
    private Long salaryRangeMax;

    private String currency;

    private String status;

    @Column(name = "posted_at")
    private LocalDate postedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extensions", columnDefinition = "jsonb")
    private Map<String, Object> extensions = new LinkedHashMap<>();

    protected JobPosting() {}

    public JobPosting(
            UUID id,
            String slug,
            String title,
            String team,
            String location,
            String employmentType,
            String commitment,
            String summary,
            List<String> responsibilities,
            List<String> requirements,
            Long salaryRangeMin,
            Long salaryRangeMax,
            String currency,
            UUID createdByUserId,
            Instant now) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.team = team;
        this.location = location;
        this.employmentType = employmentType;
        this.commitment = commitment;
        this.summary = summary;
        this.responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
        this.requirements = requirements == null ? List.of() : List.copyOf(requirements);
        this.salaryRangeMin = salaryRangeMin;
        this.salaryRangeMax = salaryRangeMax;
        this.currency = currency;
        this.createdByUserId = createdByUserId;
        this.status = JobPostingStatus.DRAFT.wire();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getTeam() { return team; }
    public String getLocation() { return location; }
    public String getEmploymentType() { return employmentType; }
    public String getCommitment() { return commitment; }
    public String getSummary() { return summary; }
    public List<String> getResponsibilities() {
        return responsibilities == null ? List.of() : List.copyOf(responsibilities);
    }
    public List<String> getRequirements() {
        return requirements == null ? List.of() : List.copyOf(requirements);
    }
    public Long getSalaryRangeMin() { return salaryRangeMin; }
    public Long getSalaryRangeMax() { return salaryRangeMax; }
    public String getCurrency() { return currency; }
    public JobPostingStatus getStatus() { return JobPostingStatus.ofWire(status); }
    public LocalDate getPostedAt() { return postedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getExtensions() {
        return extensions == null ? Collections.emptyMap() : Collections.unmodifiableMap(extensions);
    }

    public void editContent(
            String title,
            String team,
            String location,
            String employmentType,
            String commitment,
            String summary,
            List<String> responsibilities,
            List<String> requirements,
            Long salaryRangeMin,
            Long salaryRangeMax,
            String currency,
            Instant at) {
        if (title != null) this.title = title;
        if (team != null) this.team = team;
        if (location != null) this.location = location;
        if (employmentType != null) this.employmentType = employmentType;
        if (commitment != null) this.commitment = commitment;
        if (summary != null) this.summary = summary;
        if (responsibilities != null) this.responsibilities = List.copyOf(responsibilities);
        if (requirements != null) this.requirements = List.copyOf(requirements);
        if (salaryRangeMin != null) this.salaryRangeMin = salaryRangeMin;
        if (salaryRangeMax != null) this.salaryRangeMax = salaryRangeMax;
        if (currency != null) this.currency = currency;
        this.updatedAt = at;
    }

    public void publish(Instant at) {
        this.status = JobPostingStatus.PUBLISHED.wire();
        this.publishedAt = at;
        this.postedAt = LocalDate.ofInstant(at, java.time.ZoneOffset.UTC);
        this.updatedAt = at;
    }

    public void archive(Instant at) {
        this.status = JobPostingStatus.ARCHIVED.wire();
        this.archivedAt = at;
        this.updatedAt = at;
    }
}
