package com.edss.integrations.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per staff user that has connected their Google Calendar. Access
 * + refresh tokens are AES-GCM encrypted at rest.
 */
@Entity
@Table(schema = "integrations", name = "google_calendar_tokens")
public class GoogleCalendarToken {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "access_token_enc")
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc")
    private String refreshTokenEnc;

    @Column(name = "expires_at")
    private Instant expiresAt;

    private String scope;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected GoogleCalendarToken() {}

    public GoogleCalendarToken(
            UUID userId,
            String accessTokenEnc,
            String refreshTokenEnc,
            Instant expiresAt,
            String scope,
            Instant createdAt) {
        this.userId = userId;
        this.accessTokenEnc = accessTokenEnc;
        this.refreshTokenEnc = refreshTokenEnc;
        this.expiresAt = expiresAt;
        this.scope = scope;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getScope() {
        return scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rotate(String accessTokenEnc, Instant expiresAt, Instant at) {
        this.accessTokenEnc = accessTokenEnc;
        this.expiresAt = expiresAt;
        this.updatedAt = at;
    }
}
