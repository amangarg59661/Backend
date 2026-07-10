package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "milestones")
public class Milestone {

    @Id private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    private int ordinal;

    private String title;

    private String description;

    @Column(name = "amount_minor")
    private Long amountMinor;

    private String status;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected Milestone() {}

    public Milestone(
            UUID id,
            UUID projectId,
            int ordinal,
            String title,
            String description,
            Long amountMinor,
            Instant dueAt) {
        this.id = id;
        this.projectId = projectId;
        this.ordinal = ordinal;
        this.title = title;
        this.description = description;
        this.amountMinor = amountMinor;
        this.status = MilestoneStatus.PLANNED.wire();
        this.dueAt = dueAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public MilestoneStatus getStatus() {
        return MilestoneStatus.ofWire(status);
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void submit(Instant at) {
        MilestoneStatus current = getStatus();
        if (current != MilestoneStatus.PLANNED
                && current != MilestoneStatus.IN_PROGRESS
                && current != MilestoneStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException(
                    "Cannot submit milestone in status " + current.wire());
        }
        this.status = MilestoneStatus.SUBMITTED.wire();
        this.submittedAt = at;
    }

    public void applyReview(ReviewVerdict verdict, Instant at) {
        if (getStatus() != MilestoneStatus.SUBMITTED) {
            throw new IllegalStateException("Milestone must be submitted before review.");
        }
        switch (verdict) {
            case APPROVED -> {
                this.status = MilestoneStatus.APPROVED.wire();
                this.approvedAt = at;
            }
            case CHANGES_REQUESTED -> this.status = MilestoneStatus.CHANGES_REQUESTED.wire();
            case REJECTED -> this.status = MilestoneStatus.REJECTED.wire();
        }
    }
}
