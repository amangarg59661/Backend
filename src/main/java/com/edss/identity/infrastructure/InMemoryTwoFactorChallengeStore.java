package com.edss.identity.infrastructure;

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

@Component
@ConditionalOnProperty(name = "edss.features.storage.redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTwoFactorChallengeStore implements TwoFactorChallengeStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTwoFactorChallengeStore.class);
    private static final Duration TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Entry> challenges = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "60000")
    void sweepExpired() {
        Instant cutoff = Instant.now();
        int before = challenges.size();
        challenges.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(cutoff));
        int removed = before - challenges.size();
        if (removed > 0) {
            log.debug("Swept {} expired 2FA challenges ({} remain)", removed, challenges.size());
        }
    }

    @Override
    public String issue(UUID userId) {
        String challengeId = "chal_" + UUID.randomUUID();
        challenges.put(challengeId, new Entry(userId, Instant.now().plus(TTL)));
        return challengeId;
    }

    @Override
    public Optional<UUID> peek(String challengeId) {
        Entry entry = challenges.get(challengeId);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId);
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
