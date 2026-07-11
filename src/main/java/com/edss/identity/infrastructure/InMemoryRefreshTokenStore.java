package com.edss.identity.infrastructure;

import com.edss.shared.security.TokenHashing;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ConcurrentHashMap-backed refresh token store. Tokens expire lazily on
 * {@link #consume(String)}. Loses all tokens on restart — users get logged
 * out but access tokens continue to work until their 15-minute exp.
 */
@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final ConcurrentHashMap<String, Stored> tokens = new ConcurrentHashMap<>();

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
