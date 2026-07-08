package com.edss.identity.infrastructure;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "true")
public class RedisTwoFactorChallengeStore implements TwoFactorChallengeStore {

    private static final String KEY_PREFIX = "2fa:chal:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public RedisTwoFactorChallengeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String issue(UUID userId) {
        String challengeId = "chal_" + UUID.randomUUID();
        redis.opsForValue().set(KEY_PREFIX + challengeId, userId.toString(), TTL);
        return challengeId;
    }

    @Override
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
