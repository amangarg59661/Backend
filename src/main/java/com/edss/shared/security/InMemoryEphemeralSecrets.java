package com.edss.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.storage.redis-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class InMemoryEphemeralSecrets implements EphemeralSecrets {

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public String stash(String plaintext, Duration ttl) {
        String handle = TokenHashing.randomUrlBase64(24);
        entries.put(handle, new Entry(plaintext, Instant.now().plus(ttl)));
        return handle;
    }

    @Override
    public Optional<String> pop(String handle) {
        Entry entry = entries.remove(handle);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.plaintext);
    }

    /** Sweep expired entries every 60 s so a burst of unclaimed handles cannot grow the heap. */
    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
    }

    private record Entry(String plaintext, Instant expiresAt) {}
}
