package com.edss.identity.application;

import com.edss.identity.domain.PasswordResetToken;
import com.edss.identity.domain.User;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.PasswordResetTokenRepository;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.ratelimit.RateLimitDecision;
import com.edss.shared.ratelimit.RateLimiter;
import com.edss.shared.security.EphemeralSecrets;
import com.edss.shared.security.TokenHashing;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService history;
    private final OutboxWriter outbox;
    private final EphemeralSecrets ephemeralSecrets;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            PasswordHistoryService history,
            OutboxWriter outbox,
            EphemeralSecrets ephemeralSecrets,
            RateLimiter rateLimiter,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.history = history;
        this.outbox = outbox;
        this.ephemeralSecrets = ephemeralSecrets;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /**
     * Silent when the email doesn't exist — prevents user enumeration. Emits a
     * {@code password_reset_requested} event only when the user is real.
     */
    public void requestReset(String email) {
        // 3 forgot requests per email per hour absorbs typos but denies spraying.
        RateLimitDecision decision =
                rateLimiter.hit(
                        "forgot:email:" + email.toLowerCase(),
                        3,
                        java.time.Duration.ofHours(1));
        if (!decision.allowed()) {
            log.info("Forgot-password rate limit hit for email={}", email);
            // Silent absorb — never expose whether email exists.
            return;
        }
        Optional<User> maybeUser = users.findByEmailIgnoreCase(email);
        if (maybeUser.isEmpty()) {
            return;
        }
        User user = maybeUser.get();
        String plaintextToken = TokenHashing.randomUrlBase64(32);
        String hash = TokenHashing.sha256UrlBase64(plaintextToken);
        Instant now = clock.instant();
        tokens.save(new PasswordResetToken(hash, user.getId(), now.plus(TOKEN_TTL)));

        // Stash the plaintext under a short-TTL handle so the outbox row (which
        // lives across DB backups, replicas, and event replay) never carries
        // the secret. Notification listeners pop the handle to reveal the
        // token exactly once — if TTL elapses first they send a "check your
        // email" fallback prompting a fresh request.
        String handle =
                ephemeralSecrets.stash(plaintextToken, TOKEN_TTL);
        IdentityEvents.PasswordResetRequested event =
                new IdentityEvents.PasswordResetRequested(
                        UUID.randomUUID(), now, user.getId(), user.getEmail(), handle);
        outbox.append(
                "identity",
                event,
                Map.of(
                        "user_id",
                        user.getId(),
                        "email",
                        user.getEmail(),
                        "reset_token_handle",
                        handle));
    }

    public void resetPassword(String plaintextToken, String newPassword) {
        // Cap grinds against a captured hash-target.
        RateLimitDecision decision =
                rateLimiter.hit(
                        "reset:token:" + plaintextToken,
                        5,
                        java.time.Duration.ofMinutes(15));
        if (!decision.allowed()) {
            throw new ApiException(
                    ApiErrorCode.RATE_LIMITED,
                    "Too many attempts. Try again later.",
                    java.util.Map.of("retry_after", decision.retryAfter().getSeconds()));
        }
        String hash = TokenHashing.sha256UrlBase64(plaintextToken);
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

}
