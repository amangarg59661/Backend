package com.edss.identity.spi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read-only port other modules use to resolve user info without
 * touching {@code identity.*} tables. When identity extracts to its own
 * service, this becomes the gRPC/REST client interface — callers are unchanged.
 */
public interface IdentityQuery {

    Optional<UserSummary> findUser(UUID userId);

    Optional<UserSummary> findUserByEmail(String email);

    record UserSummary(
            UUID id,
            String email,
            String name,
            String avatarUrl,
            String primaryRole,
            boolean hasBothRoles,
            Instant createdAt) {}
}
