package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single 2FA method a user has enrolled. Users may have multiple rows
 * (TOTP + WhatsApp OTP + backup codes) and pick any enabled one at login.
 *
 * <p>Column usage per method:</p>
 * <ul>
 *   <li>{@link MfaMethodType#TOTP} — {@code secret_encrypted} holds the
 *       AES-GCM ciphertext of the base32 TOTP seed.</li>
 *   <li>{@link MfaMethodType#WHATSAPP_OTP} — {@code phone_e164} carries the
 *       verified phone; {@code secret_encrypted} is null.</li>
 *   <li>{@link MfaMethodType#BACKUP_CODE} — marker row only; individual
 *       codes live in {@code identity.backup_codes}.</li>
 * </ul>
 */
@Entity
@Table(schema = "identity", name = "mfa_methods")
public class MfaMethod {

    @Id private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "method_type")
    private String methodType;

    @Column(name = "secret_encrypted")
    private String secretEncrypted;

    @Column(name = "phone_e164")
    private String phoneE164;

    private boolean enabled;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    protected MfaMethod() {}

    public MfaMethod(UUID id, UUID userId, MfaMethodType type, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.methodType = type.wire();
        this.enabled = false;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MfaMethodType getMethodType() {
        return MfaMethodType.ofWire(methodType);
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setSecret(String encrypted) {
        this.secretEncrypted = encrypted;
    }

    public void setPhone(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public void markEnrolled(Instant at) {
        this.enabled = true;
        this.enrolledAt = at;
    }

    public void disable() {
        this.enabled = false;
        this.enrolledAt = null;
        this.secretEncrypted = null;
    }
}
