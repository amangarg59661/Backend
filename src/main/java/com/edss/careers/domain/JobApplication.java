package com.edss.careers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "careers", name = "job_applications")
public class JobApplication {

    @Id private UUID id;

    @Column(name = "job_posting_id")
    private UUID jobPostingId;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "applicant_email")
    private String applicantEmail;

    @Column(name = "applicant_phone")
    private String applicantPhone;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "cover_letter")
    private String coverLetter;

    private String status;

    @Column(name = "reviewer_note")
    private String reviewerNote;

    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "source_ip")
    private String sourceIp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extensions", columnDefinition = "jsonb")
    private Map<String, Object> extensions = new LinkedHashMap<>();

    protected JobApplication() {}

    public JobApplication(
            UUID id,
            UUID jobPostingId,
            String applicantName,
            String applicantEmail,
            String applicantPhone,
            String resumeUrl,
            String coverLetter,
            String sourceIp,
            Instant now) {
        this.id = id;
        this.jobPostingId = jobPostingId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.resumeUrl = resumeUrl;
        this.coverLetter = coverLetter;
        this.sourceIp = sourceIp;
        this.status = JobApplicationStatus.NEW.wire();
        this.submittedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getJobPostingId() { return jobPostingId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getApplicantPhone() { return applicantPhone; }
    public String getResumeUrl() { return resumeUrl; }
    public String getCoverLetter() { return coverLetter; }
    public JobApplicationStatus getStatus() { return JobApplicationStatus.ofWire(status); }
    public String getReviewerNote() { return reviewerNote; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getSourceIp() { return sourceIp; }
    public Map<String, Object> getExtensions() {
        return extensions == null ? Collections.emptyMap() : Collections.unmodifiableMap(extensions);
    }

    public void review(JobApplicationStatus target, String note, UUID reviewerUserId, Instant at) {
        this.status = target.wire();
        this.reviewerNote = note;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = at;
    }
}
