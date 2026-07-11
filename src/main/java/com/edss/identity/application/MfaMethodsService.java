package com.edss.identity.application;

import com.edss.identity.domain.MfaMethod;
import com.edss.identity.domain.MfaMethodType;
import com.edss.identity.domain.User;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.MfaMethodRepository;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.security.SecretCipher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multi-method 2FA management. A single user may have any combination of
 * TOTP, WhatsApp OTP, and backup codes enrolled — at login they pick which
 * enabled method to complete the challenge with.
 *
 * <p>Enrollment for TOTP + WhatsApp is two-step (start → verify). Disabling
 * any method requires the caller's current password so a hijacked session
 * cannot silently strip the second factor.</p>
 */
@Service
@Transactional
public class MfaMethodsService {

    private final UserRepository users;
    private final MfaMethodRepository methods;
    private final BackupCodeService backupCodes;
    private final WhatsappOtpService whatsappOtp;
    private final TotpService totp;
    private final SecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final OutboxWriter outbox;
    private final Clock clock;

    public MfaMethodsService(
            UserRepository users,
            MfaMethodRepository methods,
            BackupCodeService backupCodes,
            WhatsappOtpService whatsappOtp,
            TotpService totp,
            SecretCipher cipher,
            PasswordEncoder passwordEncoder,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.methods = methods;
        this.backupCodes = backupCodes;
        this.whatsappOtp = whatsappOtp;
        this.totp = totp;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
        this.outbox = outbox;
        this.clock = clock;
    }

    // ----- TOTP -----

    public TotpEnrollmentStart startTotpEnrollment(UUID userId, String userEmail) {
        String secret = totp.generateSecret();
        String encrypted = cipher.encrypt(secret);
        Instant now = clock.instant();
        MfaMethod method = getOrCreate(userId, MfaMethodType.TOTP, now);
        method.setSecret(encrypted);
        return new TotpEnrollmentStart(
                secret, totp.otpauthUri(secret, userEmail), totp.qrCodePngBase64(secret, userEmail));
    }

    public void verifyTotpEnrollment(UUID userId, String code) {
        MfaMethod method = fetchOrThrow(userId, MfaMethodType.TOTP);
        if (method.getSecretEncrypted() == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Start enrollment first.");
        }
        if (!totp.verify(cipher.decrypt(method.getSecretEncrypted()), code)) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        markEnrolled(method, MfaMethodType.TOTP);
    }

    // ----- WhatsApp OTP -----

    public void startWhatsappEnrollment(UUID userId, String phoneE164) {
        Instant now = clock.instant();
        MfaMethod method = getOrCreate(userId, MfaMethodType.WHATSAPP_OTP, now);
        method.setPhone(phoneE164);
        whatsappOtp.issueOtp(userId, phoneE164, "enroll");
    }

    public void verifyWhatsappEnrollment(UUID userId, String code) {
        MfaMethod method = fetchOrThrow(userId, MfaMethodType.WHATSAPP_OTP);
        if (!whatsappOtp.verify(userId, "enroll", code)) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        whatsappOtp.invalidate(userId, "enroll");
        markEnrolled(method, MfaMethodType.WHATSAPP_OTP);
    }

    // ----- Backup codes -----

    public List<String> regenerateBackupCodes(UUID userId) {
        Instant now = clock.instant();
        MfaMethod method = getOrCreate(userId, MfaMethodType.BACKUP_CODE, now);
        List<String> plaintext = backupCodes.regenerate(userId);
        markEnrolled(method, MfaMethodType.BACKUP_CODE);
        return plaintext;
    }

    // ----- Disable + list -----

    public void disable(UUID userId, MfaMethodType type, String currentPassword) {
        User user =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS, "Current password incorrect.");
        }
        MfaMethod method = fetchOrThrow(userId, type);
        method.disable();
        if (type == MfaMethodType.BACKUP_CODE) {
            // No plaintext to revoke — hashes stay so replay is impossible.
        }
    }

    @Transactional(readOnly = true)
    public List<MfaMethod> listEnabled(UUID userId) {
        return methods.findByUserIdAndEnabledTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<MfaMethod> listAll(UUID userId) {
        return methods.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyEnabled(UUID userId) {
        return !methods.findByUserIdAndEnabledTrue(userId).isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<MfaMethod> findEnabled(UUID userId, MfaMethodType type) {
        return methods.findByUserIdAndMethodType(userId, type.wire()).filter(MfaMethod::isEnabled);
    }

    // ----- Login-side verification helpers -----

    public boolean verifyTotpForLogin(MfaMethod method, String code) {
        if (method.getSecretEncrypted() == null) {
            return false;
        }
        return totp.verify(cipher.decrypt(method.getSecretEncrypted()), code);
    }

    public void issueWhatsappOtpForLogin(MfaMethod method) {
        whatsappOtp.issueOtp(method.getUserId(), method.getPhoneE164(), "login");
    }

    public boolean verifyWhatsappForLogin(MfaMethod method, String code) {
        boolean ok = whatsappOtp.verify(method.getUserId(), "login", code);
        if (ok) {
            whatsappOtp.invalidate(method.getUserId(), "login");
        }
        return ok;
    }

    public boolean verifyBackupCodeForLogin(UUID userId, String code) {
        return backupCodes.consume(userId, code);
    }

    // ----- Helpers -----

    private MfaMethod getOrCreate(UUID userId, MfaMethodType type, Instant now) {
        return methods.findByUserIdAndMethodType(userId, type.wire())
                .orElseGet(() -> methods.save(new MfaMethod(UUID.randomUUID(), userId, type, now)));
    }

    private MfaMethod fetchOrThrow(UUID userId, MfaMethodType type) {
        return methods.findByUserIdAndMethodType(userId, type.wire())
                .orElseThrow(
                        () ->
                                new ApiException(
                                        ApiErrorCode.NOT_FOUND,
                                        "Method not enrolled: " + type.wire()));
    }

    private void markEnrolled(MfaMethod method, MfaMethodType type) {
        Instant now = clock.instant();
        method.markEnrolled(now);
        outbox.append(
                "identity",
                new IdentityEvents.TwoFactorEnabled(
                        UUID.randomUUID(), now, method.getUserId()),
                Map.of("user_id", method.getUserId(), "method", type.wire()));
    }

    public record TotpEnrollmentStart(String secret, String otpauthUri, String qrCodePngBase64) {}
}
