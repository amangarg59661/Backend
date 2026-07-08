-- ============================================================================
-- V1__init_schema.sql
-- Bootstraps the 14 module schemas + Spring Modulith event log.
-- Every functional module gets its own schema so it can be extracted to its
-- own database without cross-schema FKs. Cross-module references are UUID-only.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS shared;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS projects;
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS commitments;
CREATE SCHEMA IF NOT EXISTS knowledge;
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS relationship;
CREATE SCHEMA IF NOT EXISTS sales;
CREATE SCHEMA IF NOT EXISTS communication;
CREATE SCHEMA IF NOT EXISTS governance;
CREATE SCHEMA IF NOT EXISTS integrations;
CREATE SCHEMA IF NOT EXISTS ai;

-- ============================================================================
-- Identity
-- ============================================================================

CREATE TABLE identity.users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    avatar_url      TEXT,
    password_hash   VARCHAR(200) NOT NULL,
    primary_role    VARCHAR(20) NOT NULL CHECK (primary_role IN ('client', 'staff')),
    has_both_roles  BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions      JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_identity_users_created_at ON identity.users (created_at DESC);

CREATE TABLE identity.user_roles (
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL CHECK (role IN ('client', 'staff')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE identity.user_permissions (
    user_id     UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    permission  VARCHAR(80) NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT ck_identity_user_permissions_format
        CHECK (permission ~ '^[a-z_]+:[a-z_*]+$')
);

CREATE TABLE identity.user_two_factor (
    user_id           UUID PRIMARY KEY REFERENCES identity.users(id) ON DELETE CASCADE,
    secret_encrypted  TEXT NOT NULL,
    enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE identity.sessions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    user_agent      TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_identity_sessions_user ON identity.sessions (user_id, last_active_at DESC);

CREATE TABLE identity.password_reset_tokens (
    token_hash  VARCHAR(128) PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);

-- ============================================================================
-- Projects
-- ============================================================================

CREATE TABLE projects.projects (
    id                UUID PRIMARY KEY,
    owner_user_id     UUID NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    status            VARCHAR(30) NOT NULL DEFAULT 'active',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions        JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_projects_owner_created ON projects.projects (owner_user_id, created_at DESC);

-- ============================================================================
-- Finance (invoices)
-- ============================================================================

CREATE TABLE finance.invoices (
    id              UUID PRIMARY KEY,
    client_user_id  UUID NOT NULL,
    number          VARCHAR(50) NOT NULL UNIQUE,
    amount_minor    BIGINT NOT NULL,
    currency        CHAR(3) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'draft',
    issued_at       TIMESTAMPTZ,
    due_at          TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions      JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_finance_invoices_client_created ON finance.invoices (client_user_id, created_at DESC);

-- ============================================================================
-- Commitments (tickets)
-- ============================================================================

CREATE TABLE commitments.tickets (
    id                    UUID PRIMARY KEY,
    raised_by_user_id     UUID NOT NULL,
    project_id            UUID,
    subject               VARCHAR(200) NOT NULL,
    description           TEXT,
    priority              VARCHAR(20) NOT NULL DEFAULT 'normal',
    status                VARCHAR(20) NOT NULL DEFAULT 'open',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions            JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_commitments_tickets_user_created
    ON commitments.tickets (raised_by_user_id, created_at DESC);

-- ============================================================================
-- Knowledge (files)
-- ============================================================================

CREATE TABLE knowledge.files (
    id              UUID PRIMARY KEY,
    owner_user_id   UUID NOT NULL,
    name            VARCHAR(300) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    mime_type       VARCHAR(200) NOT NULL,
    storage_key     TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions      JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_knowledge_files_owner_created ON knowledge.files (owner_user_id, created_at DESC);

-- ============================================================================
-- Notifications
-- ============================================================================

CREATE TABLE notifications.notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    severity    VARCHAR(20) NOT NULL CHECK (severity IN ('info', 'success', 'warning', 'critical')),
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    href        TEXT,
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions  JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_notifications_user_created
    ON notifications.notifications (user_id, created_at DESC);

-- ============================================================================
-- Per-module outbox tables (identical shape). Only functional modules ship
-- one now; skeleton modules will add theirs when they publish an event.
-- ============================================================================

CREATE TABLE identity.outbox (
    id              UUID PRIMARY KEY,
    event_type      VARCHAR(120) NOT NULL,
    event_version   INT NOT NULL,
    aggregate_type  VARCHAR(80) NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ,
    trace_id        VARCHAR(80)
);
CREATE INDEX ix_identity_outbox_unpublished
    ON identity.outbox (published_at NULLS FIRST, occurred_at);

CREATE TABLE projects.outbox (LIKE identity.outbox INCLUDING ALL);
CREATE TABLE finance.outbox (LIKE identity.outbox INCLUDING ALL);
CREATE TABLE commitments.outbox (LIKE identity.outbox INCLUDING ALL);
CREATE TABLE knowledge.outbox (LIKE identity.outbox INCLUDING ALL);
CREATE TABLE notifications.outbox (LIKE identity.outbox INCLUDING ALL);
