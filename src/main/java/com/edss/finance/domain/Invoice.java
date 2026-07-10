package com.edss.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "finance", name = "invoices")
public class Invoice {

    @Id private UUID id;

    @Column(name = "client_user_id")
    private UUID clientUserId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    private String number;

    @Column(name = "amount_minor")
    private long amountMinor;

    private String currency;

    private String status;

    private String provider;

    @Column(name = "provider_payment_intent_id")
    private String providerPaymentIntentId;

    @Column(name = "payment_link")
    private String paymentLink;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "line_items", columnDefinition = "jsonb")
    private String lineItemsJson;

    protected Invoice() {}

    public Invoice(
            UUID id,
            UUID clientUserId,
            UUID projectId,
            UUID milestoneId,
            String number,
            long amountMinor,
            String currency,
            String provider,
            String lineItemsJson,
            Instant issuedAt,
            Instant dueAt,
            Instant createdAt) {
        this.id = id;
        this.clientUserId = clientUserId;
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.number = number;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.provider = provider;
        this.lineItemsJson = lineItemsJson;
        this.status = "issued";
        this.issuedAt = issuedAt;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientUserId() {
        return clientUserId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public String getNumber() {
        return number;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderPaymentIntentId() {
        return providerPaymentIntentId;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLineItemsJson() {
        return lineItemsJson;
    }

    public void attachProviderPayment(String intentId, String paymentLink) {
        this.providerPaymentIntentId = intentId;
        this.paymentLink = paymentLink;
    }

    public void markPaid(Instant at) {
        if ("paid".equals(this.status)) {
            return;
        }
        this.status = "paid";
        this.paidAt = at;
    }

    public void markVoided(Instant at) {
        this.status = "voided";
    }
}
