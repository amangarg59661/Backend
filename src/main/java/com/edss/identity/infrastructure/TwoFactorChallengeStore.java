package com.edss.identity.infrastructure;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Short-lived 2FA challenge tokens issued when login succeeds but the user
 * has TOTP enabled. Only the {@code challenge_id} + {@code user_id} is stored;
 * the OTP itself is verified against the user's stored secret.
 */
@Component
public class TwoFactorChallengeStore {

    private static final String KEY_PREFIX = "2fa:chal:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public TwoFactorChallengeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String issue(UUID userId) {
        String challengeId = "chal_" + UUID.randomUUID();
        redis.opsForValue().set(KEY_PREFIX + challengeId, userId.toString(), TTL);
        return challengeId;
    }

    public Optional<UUID> consume(String challengeId) {
        String key = KEY_PREFIX + challengeId;
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }
        redis.delete(key);
        return Optional.of(UUID.fromString(raw));
    }
}
