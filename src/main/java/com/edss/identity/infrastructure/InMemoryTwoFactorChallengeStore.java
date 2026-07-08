package com.edss.identity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTwoFactorChallengeStore implements TwoFactorChallengeStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Entry> challenges = new ConcurrentHashMap<>();

    @Override
    public String issue(UUID userId) {
        String challengeId = "chal_" + UUID.randomUUID();
        challenges.put(challengeId, new Entry(userId, Instant.now().plus(TTL)));
        return challengeId;
    }

    @Override
    public Optional<UUID> consume(String challengeId) {
        Entry entry = challenges.remove(challengeId);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId);
    }

    private record Entry(UUID userId, Instant expiresAt) {}
}
