package com.edss.shared.security;

import java.util.UUID;

/**
 * SPI port for JWT-time session allowlist. JwtAuthFilter checks every access
 * token against this port so revoking a session (logout, password change,
 * password reset, admin action) immediately kills all outstanding access
 * tokens without waiting for their 15-minute expiry.
 *
 * <p>Implemented in the identity module by SessionService — this port lives
 * in {@code shared.security} so JwtAuthFilter can depend on it without
 * pulling {@code identity} into the shared module's dependency graph. S-18.
 */
public interface SessionAllowlist {
    boolean isActive(UUID sessionId);
}
