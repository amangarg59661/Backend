package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Signed browser token that bypasses 2FA on a specific device for a limited
 * TTL. Storing only the SHA-256 hash means a leak cannot be replayed.
 */
@Entity
@Table(schema = "identity", name = "trusted_devices")
public class TrustedDevice {

    @Id private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "device_token_hash")
    private String deviceTokenHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TrustedDevice() {}

    public TrustedDevice(
            UUID id,
            UUID userId,
            String deviceTokenHash,
            String userAgent,
            String ipAddress,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.deviceTokenHash = deviceTokenHash;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDeviceTokenHash() {
        return deviceTokenHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revoke(Instant at) {
        this.revokedAt = at;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
