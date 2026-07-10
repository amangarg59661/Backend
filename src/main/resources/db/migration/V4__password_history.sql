-- ============================================================================
-- V4__password_history.sql
-- Track the last N BCrypt-hashed passwords per user so a change/reset cannot
-- reuse a recent one. Hash column stores the same BCrypt strength-12 output
-- as identity.users.password_hash — comparison uses PasswordEncoder.matches.
-- ============================================================================

CREATE TABLE identity.password_history (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    password_hash VARCHAR(200) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_identity_password_history_user_created
    ON identity.password_history (user_id, created_at DESC);
