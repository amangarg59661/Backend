package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "onboarding_calls")
public class OnboardingCall {

    public enum Status {
        PENDING("pending"),
        SCHEDULED("scheduled"),
        DONE("done"),
        CANCELLED("cancelled");

        private final String wire;

        Status(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static Status ofWire(String v) {
            for (Status s : values()) {
                if (s.wire.equals(v)) return s;
            }
            throw new IllegalArgumentException("Unknown call status: " + v);
        }
    }

    @Id private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    private String provider;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "meeting_url")
    private String meetingUrl;

    @Column(name = "external_ref")
    private String externalRef;

    private String status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected OnboardingCall() {}

    public OnboardingCall(UUID id, UUID projectId, String provider, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.provider = provider;
        this.status = Status.PENDING.wire();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public Status getStatus() {
        return Status.ofWire(status);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markScheduled(Instant scheduledAt, String meetingUrl, String externalRef, Instant at) {
        this.scheduledAt = scheduledAt;
        this.meetingUrl = meetingUrl;
        this.externalRef = externalRef;
        this.status = Status.SCHEDULED.wire();
        this.updatedAt = at;
    }

    public void markDone(Instant at) {
        this.status = Status.DONE.wire();
        this.updatedAt = at;
    }

    public void cancel(Instant at) {
        this.status = Status.CANCELLED.wire();
        this.updatedAt = at;
    }
}
