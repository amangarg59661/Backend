-- ============================================================================
-- V3__identity_extensions.sql
-- Phase 2 identity self-service: 2FA enrollment metadata, trusted-device
-- store, session revocation as soft delete.
-- ============================================================================

-- 2FA: mark when the user finished enrolment. NULL means never enrolled.
ALTER TABLE identity.user_two_factor
    ADD COLUMN IF NOT EXISTS enrolled_at TIMESTAMPTZ;

-- 2FA: allow the row to be inserted at enrolment-start (before verify) with
-- the secret pending. `enabled` flips true after successful verify.
ALTER TABLE identity.user_two_factor
    ALTER COLUMN enabled SET DEFAULT FALSE;

-- Sessions: soft-delete + who revoked it. Revocation forces refresh-token
-- invalidation via a session_id check on the next /auth/refresh.
ALTER TABLE identity.sessions
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_by_user_id UUID;

CREATE INDEX IF NOT EXISTS ix_identity_sessions_active
    ON identity.sessions (user_id, revoked_at)
    WHERE revoked_at IS NULL;

-- Trusted devices: signed browser tokens that skip 2FA for 30 days. Stored
-- SHA-256-hashed so a Redis / DB leak cannot be replayed.
CREATE TABLE identity.trusted_devices (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    device_token_hash   VARCHAR(128) NOT NULL UNIQUE,
    user_agent          TEXT,
    ip_address          VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ
);
CREATE INDEX ix_identity_trusted_devices_user
    ON identity.trusted_devices (user_id, revoked_at)
    WHERE revoked_at IS NULL;
