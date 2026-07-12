package com.edss.shared.security;

import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed twin of {@link InMemoryEphemeralSecrets}. Uses {@code GETDEL}
 * so {@link #pop(String)} is atomic — no two consumers can each receive the
 * plaintext.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.storage.redis-enabled",
        havingValue = "true")
public class RedisEphemeralSecrets implements EphemeralSecrets {

    private static final String KEY_PREFIX = "eph:";

    private final StringRedisTemplate redis;

    public RedisEphemeralSecrets(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String stash(String plaintext, Duration ttl) {
        String handle = TokenHashing.randomUrlBase64(24);
        redis.opsForValue().set(KEY_PREFIX + handle, plaintext, ttl);
        return handle;
    }

    @Override
    public Optional<String> pop(String handle) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(KEY_PREFIX + handle));
    }
}
