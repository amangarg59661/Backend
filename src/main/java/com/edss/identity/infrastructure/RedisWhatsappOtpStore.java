package com.edss.identity.infrastructure;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.storage.redis-enabled",
        havingValue = "true")
public class RedisWhatsappOtpStore implements WhatsappOtpStore {

    private static final String KEY_PREFIX = "wa_otp:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public RedisWhatsappOtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(UUID userId, String purpose, String code) {
        redis.opsForValue().set(key(userId, purpose), code, TTL);
    }

    @Override
    public Optional<String> peek(UUID userId, String purpose) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId, purpose)));
    }

    @Override
    public void invalidate(UUID userId, String purpose) {
        redis.delete(key(userId, purpose));
    }

    private String key(UUID userId, String purpose) {
        return KEY_PREFIX + userId + ":" + purpose;
    }
}
