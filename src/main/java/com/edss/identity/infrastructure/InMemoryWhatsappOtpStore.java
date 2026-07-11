package com.edss.identity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.storage.redis-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class InMemoryWhatsappOtpStore implements WhatsappOtpStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Entry> otps = new ConcurrentHashMap<>();

    @Override
    public void put(UUID userId, String purpose, String code) {
        otps.put(key(userId, purpose), new Entry(code, Instant.now().plus(TTL)));
    }

    @Override
    public Optional<String> peek(UUID userId, String purpose) {
        Entry entry = otps.get(key(userId, purpose));
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.code);
    }

    @Override
    public void invalidate(UUID userId, String purpose) {
        otps.remove(key(userId, purpose));
    }

    private String key(UUID userId, String purpose) {
        return userId + ":" + purpose;
    }

    private record Entry(String code, Instant expiresAt) {}
}
