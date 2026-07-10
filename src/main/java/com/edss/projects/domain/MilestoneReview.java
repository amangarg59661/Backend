package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "milestone_reviews")
public class MilestoneReview {

    @Id private UUID id;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    private String verdict;

    private String comment;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at", updatable = false)
    private Instant reviewedAt;

    protected MilestoneReview() {}

    public MilestoneReview(
            UUID id,
            UUID milestoneId,
            ReviewVerdict verdict,
            String comment,
            UUID reviewedByUserId,
            Instant reviewedAt) {
        this.id = id;
        this.milestoneId = milestoneId;
        this.verdict = verdict.wire();
        this.comment = comment;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedAt = reviewedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public ReviewVerdict getVerdict() {
        return ReviewVerdict.ofWire(verdict);
    }

    public String getComment() {
        return comment;
    }

    public UUID getReviewedByUserId() {
        return reviewedByUserId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
