package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "password_history")
public class PasswordHistoryEntry {

    @Id private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected PasswordHistoryEntry() {}

    public PasswordHistoryEntry(UUID id, UUID userId, String passwordHash, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
