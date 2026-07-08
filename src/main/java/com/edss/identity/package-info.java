/**
 * Identity domain: users, sessions, authentication, 2FA, password reset.
 *
 * <p>Owns the {@code identity} Postgres schema. Publishes user lifecycle events
 * ({@code identity.user_logged_in}, {@code identity.password_reset_requested},
 * etc.) via the transactional outbox. Exposes a read-only {@code spi} port so
 * other modules can resolve users by id without touching the schema directly.</p>
 */
@org.springframework.modulith.ApplicationModule(displayName = "identity")
package com.edss.identity;
