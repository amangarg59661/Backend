package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "project_phase_history")
public class ProjectPhaseHistory {

    @Id private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "from_phase")
    private String fromPhase;

    @Column(name = "to_phase")
    private String toPhase;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    private String note;

    @Column(name = "transitioned_at", updatable = false)
    private Instant transitionedAt;

    protected ProjectPhaseHistory() {}

    public ProjectPhaseHistory(
            UUID id,
            UUID projectId,
            ProjectPhase from,
            ProjectPhase to,
            UUID actorUserId,
            String note,
            Instant transitionedAt) {
        this.id = id;
        this.projectId = projectId;
        this.fromPhase = from == null ? null : from.wire();
        this.toPhase = to.wire();
        this.actorUserId = actorUserId;
        this.note = note;
        this.transitionedAt = transitionedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getFromPhase() {
        return fromPhase;
    }

    public String getToPhase() {
        return toPhase;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getNote() {
        return note;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }
}
