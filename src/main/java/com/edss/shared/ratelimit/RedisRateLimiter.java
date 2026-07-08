package com.edss.shared.ratelimit;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter backed by Redis. First hit sets the key with the window
 * TTL; subsequent hits increment. When the counter exceeds {@code limit} the
 * caller is denied and told how long until the window rolls over.
 *
 * <p>Only wired when {@code edss.features.storage.redis-enabled=true}. Day-1 uses
 * {@link InMemoryRateLimiter} instead.</p>
 */
@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitDecision hit(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return RateLimitDecision.allow();
        }
        if (count == 1L) {
            redis.expire(key, window);
            return RateLimitDecision.allow();
        }
        if (count > limit) {
            Long ttlSeconds = redis.getExpire(key);
            Duration retryAfter =
                    ttlSeconds == null || ttlSeconds <= 0 ? window : Duration.ofSeconds(ttlSeconds);
            return RateLimitDecision.deny(retryAfter);
        }
        return RateLimitDecision.allow();
    }

    @Override
    public void reset(String key) {
        redis.delete(key);
    }
}
