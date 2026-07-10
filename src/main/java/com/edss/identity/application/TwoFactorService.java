package com.edss.identity.application;

import com.edss.identity.domain.User;
import com.edss.identity.domain.UserTwoFactor;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.identity.infrastructure.UserTwoFactorRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.security.SecretCipher;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles 2FA enrollment and disablement for the current user. Enrollment is
 * a two-step flow: {@link #startEnrollment(UUID, String)} returns a fresh
 * secret + otpauth URI + QR PNG (base64) that the frontend renders. The user
 * scans in Google/MS Authenticator and calls {@link #verifyEnrollment(UUID,
 * String)} with a live code — success flips {@code enabled=true} and emits
 * {@code identity.two_factor_enabled}.
 *
 * <p>Disable requires the current password AND a current TOTP code so a
 * hijacked session cannot silently drop the second factor.</p>
 */
@Service
@Transactional
public class TwoFactorService {

    private final UserRepository users;
    private final UserTwoFactorRepository twoFactorRepo;
    private final TotpService totp;
    private final SecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final OutboxWriter outbox;
    private final Clock clock;

    public TwoFactorService(
            UserRepository users,
            UserTwoFactorRepository twoFactorRepo,
            TotpService totp,
            SecretCipher cipher,
            PasswordEncoder passwordEncoder,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.twoFactorRepo = twoFactorRepo;
        this.totp = totp;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
        this.outbox = outbox;
        this.clock = clock;
    }

    public EnrollmentStart startEnrollment(UUID userId, String userEmail) {
        String secret = totp.generateSecret();
        String encrypted = cipher.encrypt(secret);
        Instant now = clock.instant();
        UserTwoFactor existing = twoFactorRepo.findByUserId(userId).orElse(null);
        if (existing == null) {
            twoFactorRepo.save(new UserTwoFactor(userId, encrypted, now));
        } else {
            existing.rotateSecret(encrypted, now);
        }
        return new EnrollmentStart(secret, totp.otpauthUri(secret, userEmail), totp.qrCodePngBase64(secret, userEmail));
    }

    public void verifyEnrollment(UUID userId, String code) {
        UserTwoFactor row =
                twoFactorRepo.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.VALIDATION_FAILED,
                                                "Start enrollment first."));
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totp.verify(secret, code)) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        Instant now = clock.instant();
        row.markEnrolled(now);
        outbox.append(
                "identity",
                new IdentityEvents.TwoFactorEnabled(UUID.randomUUID(), now, userId),
                Map.of("user_id", userId));
    }

    public void disable(UUID userId, String currentPassword, String currentCode) {
        User user =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS, "Current password incorrect.");
        }
        UserTwoFactor row =
                twoFactorRepo.findByUserId(userId)
                        .filter(UserTwoFactor::isEnabled)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.VALIDATION_FAILED,
                                                "2FA is not enabled."));
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totp.verify(secret, currentCode)) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        row.disable();
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return twoFactorRepo.findByUserId(userId).map(UserTwoFactor::isEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<String> decryptSecret(UUID userId) {
        return twoFactorRepo.findByUserId(userId)
                .filter(UserTwoFactor::isEnabled)
                .map(row -> cipher.decrypt(row.getSecretEncrypted()));
    }

    public record EnrollmentStart(String secret, String otpauthUri, String qrCodePngBase64) {}
}
