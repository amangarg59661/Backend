package com.edss.shared.security;

import java.time.Duration;
import java.util.Optional;

/**
 * Short-TTL store for secrets that must reach a downstream module but must
 * never persist through the outbox / event log / DB backup. The publisher
 * stashes the plaintext under a random handle; the outbox row carries the
 * handle only; the consumer pops the handle exactly once.
 *
 * <p>If the consumer runs after TTL expiry (relay lag + retry), the handle
 * returns empty and the consumer must degrade gracefully — e.g. an email
 * that omits the token and asks the user to re-request.</p>
 */
public interface EphemeralSecrets {

    /** Stash a plaintext secret and return an opaque handle. */
    String stash(String plaintext, Duration ttl);

    /** Retrieve and delete the plaintext. Empty when handle unknown or expired. */
    Optional<String> pop(String handle);
}
