package com.edss.shared.ratelimit;

import java.time.Duration;

/**
 * Fixed-window rate limit primitive. Two impls exist:
 * <ul>
 *   <li>In-memory (default) — single-node, resets on restart. Fine for day-1.</li>
 *   <li>Redis-backed — survives restarts, shared across instances. Enable via
 *       {@code edss.features.storage.redis-enabled=true} when scaling out.</li>
 * </ul>
 */
public interface RateLimiter {

    RateLimitDecision hit(String key, int limit, Duration window);

    void reset(String key);
}
