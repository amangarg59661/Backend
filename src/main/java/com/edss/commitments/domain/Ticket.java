package com.edss.commitments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "commitments", name = "tickets")
public class Ticket {

    @Id private UUID id;

    @Column(name = "raised_by_user_id")
    private UUID raisedByUserId;

    @Column(name = "project_id")
    private UUID projectId;

    private String subject;

    private String description;

    private String priority;

    private String status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Ticket() {}

    public UUID getId() {
        return id;
    }

    public UUID getRaisedByUserId() {
        return raisedByUserId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
