package com.edss.shared.security;

import java.util.UUID;

/**
 * Principal attached to the Spring Security context after JWT validation.
 * Carries the fields needed by controllers and permission checks without
 * requiring a DB lookup on every request.
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        String primaryRole,
        boolean hasBothRoles,
        UUID sessionId) {

    public boolean isStaff() {
        return "staff".equals(primaryRole) || hasBothRoles;
    }
}
