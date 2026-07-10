package com.edss.identity.application;

import com.edss.identity.domain.PasswordHistoryEntry;
import com.edss.identity.infrastructure.PasswordHistoryRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blocks reuse of the last N (default 3) passwords per user. Used by both
 * change-password and reset-password so the rule is uniform. The current
 * password_hash on identity.users is treated as an implicit entry, so the
 * user cannot pass "old" as new either.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PasswordHistoryService {

    public static final int RETAINED = 3;

    private final PasswordHistoryRepository history;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordHistoryService(
            PasswordHistoryRepository history, PasswordEncoder passwordEncoder, Clock clock) {
        this.history = history;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Throws VALIDATION_FAILED if the plaintext matches the current stored hash
     * or any of the last {@link #RETAINED} historical hashes.
     */
    public void rejectIfReused(UUID userId, String currentHash, String candidatePlaintext) {
        if (currentHash != null && passwordEncoder.matches(candidatePlaintext, currentHash)) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "New password must differ from the last " + RETAINED + " passwords.");
        }
        List<PasswordHistoryEntry> recent =
                history.findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.Limit.of(RETAINED));
        for (PasswordHistoryEntry entry : recent) {
            if (passwordEncoder.matches(candidatePlaintext, entry.getPasswordHash())) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_FAILED,
                        "New password must differ from the last " + RETAINED + " passwords.");
            }
        }
    }

    /**
     * Persist the freshly-set BCrypt hash, then trim the table so at most
     * {@link #RETAINED} rows remain per user.
     */
    public void recordNewHash(UUID userId, String newHash) {
        Instant now = clock.instant();
        history.save(new PasswordHistoryEntry(UUID.randomUUID(), userId, newHash, now));
        history.trimToLatest(userId, RETAINED);
    }
}
