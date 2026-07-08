package com.edss.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived 2FA challenge tokens issued when login succeeds but the user
 * has TOTP enabled. Two impls (in-memory / Redis) switch on
 * {@code edss.features.storage.redis-enabled}.
 */
public interface TwoFactorChallengeStore {

    String issue(UUID userId);

    Optional<UUID> consume(String challengeId);
}
