package com.edss.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "finance", name = "payment_webhook_events")
public class PaymentWebhookEvent {

    @Id private UUID id;

    private String provider;

    @Column(name = "external_event_id")
    private String externalEventId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    private String status;

    private String error;

    protected PaymentWebhookEvent() {}

    public PaymentWebhookEvent(
            UUID id,
            String provider,
            String externalEventId,
            UUID invoiceId,
            String payload,
            Instant receivedAt) {
        this.id = id;
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.invoiceId = invoiceId;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.status = "pending";
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public void markApplied(Instant at) {
        this.status = "applied";
        this.processedAt = at;
    }

    public void markDead(Instant at, String error) {
        this.status = "dead";
        this.processedAt = at;
        this.error = error;
    }
}
