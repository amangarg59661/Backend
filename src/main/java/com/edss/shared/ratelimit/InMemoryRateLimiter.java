package com.edss.shared.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Single-node, in-memory fixed-window counter. Buckets expire lazily on hit.
 * Loses state on restart — acceptable for day-1 single-instance deployments.
 */
@Component
@ConditionalOnProperty(name = "edss.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision hit(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Bucket bucket =
                buckets.compute(
                        key,
                        (k, existing) -> {
                            if (existing == null || !now.isBefore(existing.expiresAt)) {
                                return new Bucket(1, now.plus(window));
                            }
                            return new Bucket(existing.count + 1, existing.expiresAt);
                        });
        if (bucket.count > limit) {
            return RateLimitDecision.deny(Duration.between(now, bucket.expiresAt));
        }
        return RateLimitDecision.allow();
    }

    @Override
    public void reset(String key) {
        buckets.remove(key);
    }

    private record Bucket(int count, Instant expiresAt) {}
}
