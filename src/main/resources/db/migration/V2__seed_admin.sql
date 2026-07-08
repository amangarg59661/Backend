-- ============================================================================
-- V2__seed_admin.sql
-- Idempotent seed of the initial admin user. The BCrypt hash is computed by
-- application code (AdminSeedRunner), not by this migration — this file only
-- reserves the row and grants full permissions once the app inserts it. If
-- you need a hard-coded seed row for a fresh DB, replace the WHERE clause
-- and add an INSERT ... ON CONFLICT block with a pre-computed hash.
-- ============================================================================

-- No-op: the admin user is inserted at application startup by
-- com.edss.identity.application.AdminSeedRunner so the BCrypt hash uses the
-- runtime PasswordEncoder (strength 12) and pulls its plaintext from env.
-- The file exists so Flyway records V2 in its history and future migrations
-- can advance past it.

SELECT 1;
