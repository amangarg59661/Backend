package com.edss.identity.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Opaque refresh tokens stored in Redis. The token itself is a 32-byte random
 * value; the key is {@code rt:<sha256(token)>} so a leak of Redis contents
 * can't be replayed.
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "rt:";
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RefreshTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public IssuedRefresh issue(UUID userId, UUID sessionId, Duration ttl) {
        String token = randomToken();
        String key = KEY_PREFIX + hash(token);
        Instant expiresAt = Instant.now().plus(ttl);
        Stored stored = new Stored(userId, sessionId, expiresAt);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(stored), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return new IssuedRefresh(token, expiresAt);
    }

    public Optional<Stored> consume(String token) {
        String key = KEY_PREFIX + hash(token);
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

    public void revoke(String token) {
        redis.delete(KEY_PREFIX + hash(token));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return ENC.encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ENC.encodeToString(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record IssuedRefresh(String token, Instant expiresAt) {}

    public record Stored(UUID userId, UUID sessionId, Instant expiresAt) {}
}
