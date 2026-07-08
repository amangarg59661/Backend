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

    private String number;

    @Column(name = "amount_minor")
    private long amountMinor;

    private String currency;

    private String status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Invoice() {}

    public UUID getId() {
        return id;
    }

    public UUID getClientUserId() {
        return clientUserId;
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

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
