package com.edss.commitments.domain;

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

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "is_maintenance")
    private boolean isMaintenance;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extensions", columnDefinition = "jsonb")
    private Map<String, Object> extensions = new LinkedHashMap<>();

    protected Ticket() {}

    public Ticket(
            UUID id,
            UUID raisedByUserId,
            UUID projectId,
            String subject,
            String description,
            String priority,
            boolean isMaintenance,
            Instant createdAt) {
        this.id = id;
        this.raisedByUserId = raisedByUserId;
        this.projectId = projectId;
        this.subject = subject;
        this.description = description;
        this.priority = priority == null ? "normal" : priority;
        this.status = TicketStatus.OPEN.wire();
        this.isMaintenance = isMaintenance;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

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

    public TicketStatus getStatus() {
        return TicketStatus.ofWire(status);
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public boolean isMaintenance() {
        return isMaintenance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, Object> getExtensions() {
        return extensions == null ? Collections.emptyMap() : Collections.unmodifiableMap(extensions);
    }

    public void assign(UUID assigneeUserId, Instant at) {
        this.assigneeUserId = assigneeUserId;
        this.updatedAt = at;
    }

    public void changeStatus(TicketStatus target, Instant at) {
        this.status = target.wire();
        this.updatedAt = at;
    }

    public void touch(Instant at) {
        this.updatedAt = at;
    }
}
