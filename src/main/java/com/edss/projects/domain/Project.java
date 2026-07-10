package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "projects")
public class Project {

    @Id private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    private String title;

    private String description;

    private String status;

    private String phase;

    @Column(name = "billing_model")
    private String billingModel;

    @Column(name = "maintenance_duration_days")
    private Integer maintenanceDurationDays;

    @Column(name = "maintenance_starts_at")
    private Instant maintenanceStartsAt;

    @Column(name = "maintenance_ends_at")
    private Instant maintenanceEndsAt;

    @Column(name = "total_amount_minor")
    private Long totalAmountMinor;

    private String currency;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Project() {}

    public Project(
            UUID id,
            UUID ownerUserId,
            String title,
            String description,
            BillingModel billingModel,
            Integer maintenanceDurationDays,
            Long totalAmountMinor,
            String currency,
            Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.status = "active";
        this.phase = ProjectPhase.DISCUSSION.wire();
        this.billingModel = billingModel.wire();
        this.maintenanceDurationDays = maintenanceDurationDays;
        this.totalAmountMinor = totalAmountMinor;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public ProjectPhase getPhase() {
        return ProjectPhase.ofWire(phase);
    }

    public BillingModel getBillingModel() {
        return BillingModel.ofWire(billingModel);
    }

    public Integer getMaintenanceDurationDays() {
        return maintenanceDurationDays;
    }

    public Instant getMaintenanceStartsAt() {
        return maintenanceStartsAt;
    }

    public Instant getMaintenanceEndsAt() {
        return maintenanceEndsAt;
    }

    public Long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void transitionTo(ProjectPhase target, Instant at) {
        this.phase = target.wire();
        this.updatedAt = at;
        if (target == ProjectPhase.MAINTENANCE && maintenanceDurationDays != null) {
            this.maintenanceStartsAt = at;
            this.maintenanceEndsAt = at.plus(Duration.ofDays(maintenanceDurationDays));
        }
        if (target == ProjectPhase.CLOSED) {
            this.status = "closed";
        }
    }

    public boolean isInMaintenanceWindow(Instant now) {
        return maintenanceStartsAt != null
                && maintenanceEndsAt != null
                && !now.isBefore(maintenanceStartsAt)
                && now.isBefore(maintenanceEndsAt);
    }
}
