-- ============================================================================
-- V5__permissions_and_role_seed.sql
-- Loosen the identity.user_permissions regex to accept the ":own" ownership
-- suffix used by the PermissionEvaluator (e.g. projects:project:read:own).
-- ============================================================================

ALTER TABLE identity.user_permissions
    DROP CONSTRAINT IF EXISTS ck_identity_user_permissions_format;

ALTER TABLE identity.user_permissions
    ADD CONSTRAINT ck_identity_user_permissions_format
        CHECK (permission ~ '^[a-z_]+:[a-z_*]+(:[a-z_*]+)?(:own)?$');
