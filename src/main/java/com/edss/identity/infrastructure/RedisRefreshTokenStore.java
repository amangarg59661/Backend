package com.edss.identity.infrastructure;

import com.edss.shared.security.TokenHashing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed refresh token store. Keyed by {@code rt:<sha256(token)>}; TTL
 * enforced by Redis itself. Only wired when {@code edss.features.storage.redis-enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "true")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "rt:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisRefreshTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public IssuedRefresh issue(UUID userId, UUID sessionId, Duration ttl) {
        String token = TokenHashing.randomUrlBase64(32);
        String key = KEY_PREFIX + TokenHashing.sha256UrlBase64(token);
        Instant expiresAt = Instant.now().plus(ttl);
        Stored stored = new Stored(userId, sessionId, expiresAt);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(stored), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return new IssuedRefresh(token, expiresAt);
    }

    @Override
    public Optional<Stored> consume(String token) {
        String key = KEY_PREFIX + TokenHashing.sha256UrlBase64(token);
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }
        redis.delete(key);
        try {
            return Optional.of(objectMapper.readValue(raw, Stored.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void revoke(String token) {
        redis.delete(KEY_PREFIX + TokenHashing.sha256UrlBase64(token));
    }
}
