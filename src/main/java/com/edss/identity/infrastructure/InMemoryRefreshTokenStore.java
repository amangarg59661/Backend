package com.edss.identity.infrastructure;

import com.edss.shared.security.TokenHashing;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ConcurrentHashMap-backed refresh token store. Tokens expire lazily on
 * {@link #consume(String)}. Loses all tokens on restart — users get logged
 * out but access tokens continue to work until their 15-minute exp.
 */
@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRefreshTokenStore.class);

    private final ConcurrentHashMap<String, Stored> tokens = new ConcurrentHashMap<>();

    /**
     * Removes expired entries on a fixed cadence so the map does not grow
     * unbounded on a long-lived JVM. Lazy expiry on {@link #consume(String)}
     * only trims entries a user actually retries, so an idle refresh token
     * would otherwise sit until process death. Sweep every 5 minutes.
     */
    @Scheduled(fixedDelayString = "300000")
    void sweepExpired() {
        Instant cutoff = Instant.now();
        int before = tokens.size();
        tokens.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(cutoff));
        int removed = before - tokens.size();
        if (removed > 0) {
            log.debug("Swept {} expired refresh tokens ({} remain)", removed, tokens.size());
        }
    }

    @Override
    public IssuedRefresh issue(UUID userId, UUID sessionId, Duration ttl) {
        String token = TokenHashing.randomUrlBase64(32);
        Instant expiresAt = Instant.now().plus(ttl);
        tokens.put(TokenHashing.sha256UrlBase64(token), new Stored(userId, sessionId, expiresAt));
        return new IssuedRefresh(token, expiresAt);
    }

    @Override
    public Optional<Stored> consume(String token) {
        Stored stored = tokens.remove(TokenHashing.sha256UrlBase64(token));
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(stored);
    }

    @Override
    public void revoke(String token) {
        tokens.remove(TokenHashing.sha256UrlBase64(token));
    }
}
