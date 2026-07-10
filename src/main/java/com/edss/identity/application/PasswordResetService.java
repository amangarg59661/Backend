package com.edss.identity.application;

import com.edss.identity.domain.PasswordResetToken;
import com.edss.identity.domain.User;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.PasswordResetTokenRepository;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService history;
    private final OutboxWriter outbox;
    private final Clock clock;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            PasswordHistoryService history,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.history = history;
        this.outbox = outbox;
        this.clock = clock;
    }

    /**
     * Silent when the email doesn't exist — prevents user enumeration. Emits a
     * {@code password_reset_requested} event only when the user is real.
     */
    public void requestReset(String email) {
        Optional<User> maybeUser = users.findByEmailIgnoreCase(email);
        if (maybeUser.isEmpty()) {
            return;
        }
        User user = maybeUser.get();
        String plaintextToken = randomToken();
        String hash = sha256(plaintextToken);
        Instant now = clock.instant();
        tokens.save(new PasswordResetToken(hash, user.getId(), now.plus(TOKEN_TTL)));

        IdentityEvents.PasswordResetRequested event =
                new IdentityEvents.PasswordResetRequested(
                        UUID.randomUUID(), now, user.getId(), user.getEmail(), plaintextToken);
        outbox.append(
                "identity",
                event,
                Map.of(
                        "user_id",
                        user.getId(),
                        "email",
                        user.getEmail(),
                        "reset_token",
                        plaintextToken));
    }

    public void resetPassword(String plaintextToken, String newPassword) {
        String hash = sha256(plaintextToken);
        PasswordResetToken token =
                tokens.findByTokenHash(hash)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.VALIDATION_FAILED,
                                                "Invalid or expired token."));
        Instant now = clock.instant();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Invalid or expired token.");
        }
        User user =
                users.findById(token.getUserId())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.VALIDATION_FAILED,
                                                "Invalid or expired token."));
        history.rejectIfReused(user.getId(), user.getPasswordHash(), newPassword);
        String newHash = passwordEncoder.encode(newPassword);
        user.changePasswordHash(newHash, now);
        history.recordNewHash(user.getId(), newHash);
        token.markUsed(now);
        log.info("Password reset for userId={}", user.getId());
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return ENC.encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ENC.encodeToString(out);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
