package com.edss.identity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Opaque refresh tokens keyed by SHA-256 hash. Two impls:
 * <ul>
 *   <li>In-memory (default) — single-node, lost on restart. Fine for day-1.</li>
 *   <li>Redis-backed — shared across instances, survives restarts. Enable via
 *       {@code edss.redis.enabled=true}.</li>
 * </ul>
 */
public interface RefreshTokenStore {

    IssuedRefresh issue(UUID userId, UUID sessionId, Duration ttl);

    Optional<Stored> consume(String token);

    void revoke(String token);

    record IssuedRefresh(String token, Instant expiresAt) {}

    record Stored(UUID userId, UUID sessionId, Instant expiresAt) {}
}
