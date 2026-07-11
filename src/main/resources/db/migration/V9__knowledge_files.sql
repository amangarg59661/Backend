-- ============================================================================
-- V9__knowledge_files.sql
-- File uploads: metadata + presigned-URL session tracking, per-project and
-- per-milestone scoping so upload lists can be filtered cheaply. knowledge
-- outbox.
-- ============================================================================

ALTER TABLE knowledge.files
    ADD COLUMN IF NOT EXISTS bucket VARCHAR(80),
    ADD COLUMN IF NOT EXISTS project_id UUID,
    ADD COLUMN IF NOT EXISTS milestone_id UUID,
    ADD COLUMN IF NOT EXISTS kind VARCHAR(40) NOT NULL DEFAULT 'general'
        CHECK (kind IN ('project_asset', 'milestone_deliverable', 'general', 'contract', 'avatar'));

ALTER TABLE knowledge.files ALTER COLUMN kind DROP DEFAULT;

CREATE INDEX IF NOT EXISTS ix_knowledge_files_project
    ON knowledge.files (project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_knowledge_files_milestone
    ON knowledge.files (milestone_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_knowledge_files_kind
    ON knowledge.files (kind, created_at DESC);

-- Upload sessions: presigned URL tracking so orphan uploads can be reaped.
CREATE TABLE knowledge.file_uploads (
    id                  UUID PRIMARY KEY,
    upload_id           VARCHAR(120) NOT NULL UNIQUE,
    owner_user_id       UUID NOT NULL,
    bucket              VARCHAR(80) NOT NULL,
    storage_key         TEXT NOT NULL,
    presigned_url       TEXT NOT NULL,
    content_type        VARCHAR(200),
    kind                VARCHAR(40) NOT NULL
        CHECK (kind IN ('project_asset', 'milestone_deliverable', 'general', 'contract', 'avatar')),
    project_id          UUID,
    milestone_id        UUID,
    original_name       VARCHAR(300),
    expected_size       BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL,
    completed_at        TIMESTAMPTZ
);
CREATE INDEX ix_knowledge_file_uploads_owner
    ON knowledge.file_uploads (owner_user_id, created_at DESC);

CREATE TABLE knowledge.outbox (LIKE identity.outbox INCLUDING ALL);
