-- ============================================================================
-- V13__mfa_methods.sql
-- Multi-method 2FA. Replaces the single-row user_two_factor with a
-- per-method table so a user can have TOTP + WhatsApp OTP + backup codes
-- all enrolled at once and pick whichever they want at login time.
-- ============================================================================

CREATE TABLE identity.mfa_methods (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    method_type         VARCHAR(30) NOT NULL
        CHECK (method_type IN ('totp', 'whatsapp_otp', 'backup_code')),
    secret_encrypted    TEXT,
    phone_e164          VARCHAR(20),
    enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    enrolled_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, method_type)
);
CREATE INDEX ix_identity_mfa_methods_user_enabled
    ON identity.mfa_methods (user_id, enabled)
    WHERE enabled = TRUE;

CREATE TABLE identity.backup_codes (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    code_hash       VARCHAR(128) NOT NULL,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    used_at         TIMESTAMPTZ,
    UNIQUE (user_id, code_hash)
);
CREATE INDEX ix_identity_backup_codes_user_active
    ON identity.backup_codes (user_id)
    WHERE used_at IS NULL;

-- Migrate existing single-row TOTP enrollments into the new table.
-- ON CONFLICT keeps the migration idempotent on partial reruns — otherwise
-- any pre-existing (user_id, 'totp') row aborts the whole migration and
-- leaves V13 half-applied. A-05 fix from the audit remediation branch.
INSERT INTO identity.mfa_methods
    (id, user_id, method_type, secret_encrypted, enabled, enrolled_at, created_at)
SELECT
    gen_random_uuid(),
    user_id,
    'totp',
    secret_encrypted,
    enabled,
    enrolled_at,
    created_at
FROM identity.user_two_factor
ON CONFLICT (user_id, method_type) DO NOTHING;

DROP TABLE identity.user_two_factor;
