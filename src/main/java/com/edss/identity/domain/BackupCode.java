package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One single-use backup code. Stored SHA-256 hashed so the raw code is shown
 * to the user exactly once at generation and never recoverable from the DB.
 */
@Entity
@Table(schema = "identity", name = "backup_codes")
public class BackupCode {

    @Id private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected BackupCode() {}

    public BackupCode(UUID id, UUID userId, String codeHash, Instant generatedAt) {
        this.id = id;
        this.userId = userId;
        this.codeHash = codeHash;
        this.generatedAt = generatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Instant at) {
        this.usedAt = at;
    }
}
