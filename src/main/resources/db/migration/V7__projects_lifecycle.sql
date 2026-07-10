-- ============================================================================
-- V7__projects_lifecycle.sql
-- Full project lifecycle: 14-state phase machine, milestones + reviews,
-- contracts (unsigned + signed), onboarding calls, phase history audit log,
-- per-module outbox.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- projects.projects — add lifecycle + billing columns.
-- ----------------------------------------------------------------------------
ALTER TABLE projects.projects
    ADD COLUMN IF NOT EXISTS phase VARCHAR(40) NOT NULL DEFAULT 'discussion'
        CHECK (phase IN (
            'discussion', 'contract_pending', 'contract_signed',
            'onboarding_scheduled', 'onboarding_complete',
            'advance_invoiced', 'assets_pending', 'assets_received',
            'in_progress', 'client_review',
            'final_submission', 'final_invoiced',
            'maintenance', 'closed'
        )),
    ADD COLUMN IF NOT EXISTS billing_model VARCHAR(20) NOT NULL DEFAULT 'whole_project'
        CHECK (billing_model IN ('per_milestone', 'whole_project')),
    ADD COLUMN IF NOT EXISTS maintenance_duration_days INT,
    ADD COLUMN IF NOT EXISTS maintenance_starts_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS maintenance_ends_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS total_amount_minor BIGINT,
    ADD COLUMN IF NOT EXISTS currency CHAR(3);

-- ----------------------------------------------------------------------------
-- Phase transition history — every advance / reset writes one row.
-- ----------------------------------------------------------------------------
CREATE TABLE projects.project_phase_history (
    id                  UUID PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES projects.projects(id) ON DELETE CASCADE,
    from_phase          VARCHAR(40),
    to_phase            VARCHAR(40) NOT NULL,
    actor_user_id       UUID NOT NULL,
    note                TEXT,
    transitioned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_projects_phase_history_project
    ON projects.project_phase_history (project_id, transitioned_at DESC);

-- ----------------------------------------------------------------------------
-- Contracts — unsigned uploaded by staff, signed uploaded back by client.
-- Both live forever, downloadable by both sides.
-- ----------------------------------------------------------------------------
CREATE TABLE projects.contracts (
    id                  UUID PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES projects.projects(id) ON DELETE CASCADE,
    kind                VARCHAR(20) NOT NULL CHECK (kind IN ('unsigned', 'signed')),
    storage_key         TEXT NOT NULL,
    sha256              VARCHAR(128) NOT NULL,
    uploaded_by_user_id UUID NOT NULL,
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions          JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_projects_contracts_project_kind
    ON projects.contracts (project_id, kind, uploaded_at DESC);

-- ----------------------------------------------------------------------------
-- Milestones and their reviews. Per-project ordinal keeps ordering stable
-- even when staff renames milestones.
-- ----------------------------------------------------------------------------
CREATE TABLE projects.milestones (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects.projects(id) ON DELETE CASCADE,
    ordinal         INT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    amount_minor    BIGINT,
    status          VARCHAR(30) NOT NULL DEFAULT 'planned'
        CHECK (status IN (
            'planned', 'in_progress', 'submitted',
            'changes_requested', 'approved', 'rejected'
        )),
    due_at          TIMESTAMPTZ,
    submitted_at    TIMESTAMPTZ,
    approved_at     TIMESTAMPTZ,
    extensions      JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (project_id, ordinal)
);
CREATE INDEX ix_projects_milestones_project_ordinal
    ON projects.milestones (project_id, ordinal);

CREATE TABLE projects.milestone_reviews (
    id                  UUID PRIMARY KEY,
    milestone_id        UUID NOT NULL REFERENCES projects.milestones(id) ON DELETE CASCADE,
    verdict             VARCHAR(30) NOT NULL
        CHECK (verdict IN ('approved', 'changes_requested', 'rejected')),
    comment             TEXT,
    reviewed_by_user_id UUID NOT NULL,
    reviewed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_projects_milestone_reviews_milestone
    ON projects.milestone_reviews (milestone_id, reviewed_at DESC);

-- ----------------------------------------------------------------------------
-- Onboarding calls — one per project. Provider + booking metadata.
-- ----------------------------------------------------------------------------
CREATE TABLE projects.onboarding_calls (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL UNIQUE REFERENCES projects.projects(id) ON DELETE CASCADE,
    provider        VARCHAR(20) NOT NULL CHECK (provider IN ('calcom', 'calendly', 'manual')),
    scheduled_at    TIMESTAMPTZ,
    meeting_url     TEXT,
    external_ref    VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'scheduled', 'done', 'cancelled')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Outbox mirror for the projects schema.
-- ----------------------------------------------------------------------------
CREATE TABLE projects.outbox (LIKE identity.outbox INCLUDING ALL);
