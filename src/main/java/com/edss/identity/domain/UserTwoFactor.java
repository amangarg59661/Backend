package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Per-user TOTP configuration. Row exists after the user starts enrollment
 * (secret populated, {@code enabled=false}). Flips to {@code enabled=true}
 * once the user verifies a code. Disable resets {@code enabled=false} but
 * keeps the row until the user re-enrolls with a fresh secret.
 */
@Entity
@Table(schema = "identity", name = "user_two_factor")
public class UserTwoFactor {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "secret_encrypted")
    private String secretEncrypted;

    private boolean enabled;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    protected UserTwoFactor() {}

    public UserTwoFactor(UUID userId, String secretEncrypted, Instant createdAt) {
        this.userId = userId;
        this.secretEncrypted = secretEncrypted;
        this.enabled = false;
        this.createdAt = createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public void rotateSecret(String newEncrypted, Instant at) {
        this.secretEncrypted = newEncrypted;
        this.enabled = false;
        this.enrolledAt = null;
        this.createdAt = at;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void markEnrolled(Instant at) {
        this.enabled = true;
        this.enrolledAt = at;
    }

    public void disable() {
        this.enabled = false;
        this.enrolledAt = null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
