package com.edss.identity.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-module port for creating users. Only way other modules can add rows
 * to {@code identity.users} — direct DB writes across schema boundaries are
 * forbidden. When identity extracts to its own service, this becomes the
 * remote client interface with no caller changes.
 */
public interface IdentityUserProvisioning {

    /**
     * Create an invited user in an unusable-password state and mint a
     * one-time reset token so the recipient can set their real password on
     * first sign-in.
     *
     * @param email       login email; unique
     * @param name        display name
     * @param primaryRole role wire value ({@code client}, {@code staff}, ...)
     * @return invite result including the plaintext reset token (single-use)
     */
    InviteResult createInvited(String email, String name, String primaryRole);

    record InviteResult(UUID userId, String inviteToken, Instant expiresAt) {}

    class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("A user with email " + email + " already exists.");
        }
    }
}
