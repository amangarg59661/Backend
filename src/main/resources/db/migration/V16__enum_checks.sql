-- ============================================================================
-- V16__enum_checks.sql
-- A-11: guard the state / kind columns that are stored as VARCHAR so bad
-- values fail at INSERT rather than sneaking past application-level checks
-- via /* raw SQL, bad casts, JDBC quirks. CHECK constraints are the cheapest
-- possible defense-in-depth for enum-shaped columns.
--
-- All constraints are IF NOT EXISTS via anonymous DO blocks so re-application
-- is safe (per ADR-0004 idempotency rules).
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_projects_phase'
    ) THEN
        ALTER TABLE projects.projects
            ADD CONSTRAINT ck_projects_phase CHECK (phase IN (
                'discussion',
                'contract_pending',
                'contract_signed',
                'onboarding_scheduled',
                'onboarding_complete',
                'advance_invoiced',
                'assets_pending',
                'assets_received',
                'in_progress',
                'client_review',
                'final_submission',
                'final_invoiced',
                'maintenance',
                'closed'
            ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_projects_billing_model'
    ) THEN
        ALTER TABLE projects.projects
            ADD CONSTRAINT ck_projects_billing_model CHECK (
                billing_model IS NULL OR billing_model IN ('per_milestone', 'whole_project')
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_milestones_status'
    ) THEN
        ALTER TABLE projects.milestones
            ADD CONSTRAINT ck_milestones_status CHECK (status IN (
                'planned', 'in_progress', 'submitted',
                'changes_requested', 'approved', 'rejected'
            ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_invoices_status'
    ) THEN
        ALTER TABLE finance.invoices
            ADD CONSTRAINT ck_invoices_status CHECK (status IN (
                'issued', 'paid', 'voided'
            ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_invoices_provider'
    ) THEN
        ALTER TABLE finance.invoices
            ADD CONSTRAINT ck_invoices_provider CHECK (provider IN (
                'stripe', 'razorpay', 'manual'
            ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_inquiries_status'
    ) THEN
        ALTER TABLE relationship.inquiries
            ADD CONSTRAINT ck_inquiries_status CHECK (status IN (
                'new', 'in_review', 'converted', 'rejected'
            ));
    END IF;

    -- notifications.severity already has an inline CHECK from V1 covering
    -- ('info', 'success', 'warning', 'critical'); nothing to add here.

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_primary_role'
    ) THEN
        ALTER TABLE identity.users
            ADD CONSTRAINT ck_users_primary_role CHECK (primary_role IN (
                'client', 'staff'
            ));
    END IF;
END$$;

-- ROLLBACK:
-- DO $$
-- BEGIN
--     ALTER TABLE projects.projects DROP CONSTRAINT IF EXISTS ck_projects_phase;
--     ALTER TABLE projects.projects DROP CONSTRAINT IF EXISTS ck_projects_billing_model;
--     ALTER TABLE projects.milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
--     ALTER TABLE finance.invoices DROP CONSTRAINT IF EXISTS ck_invoices_status;
--     ALTER TABLE finance.invoices DROP CONSTRAINT IF EXISTS ck_invoices_provider;
--     ALTER TABLE relationship.inquiries DROP CONSTRAINT IF EXISTS ck_inquiries_status;
--     ALTER TABLE notifications.notifications DROP CONSTRAINT IF EXISTS ck_notifications_severity;
--     ALTER TABLE identity.users DROP CONSTRAINT IF EXISTS ck_users_primary_role;
-- END$$;
