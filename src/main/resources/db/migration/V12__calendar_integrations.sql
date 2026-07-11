-- ============================================================================
-- V12__calendar_integrations.sql
-- Per-staff Google Calendar OAuth tokens + calendar webhook idempotency
-- store. onboarding_calls stays in the projects schema (V7); this file only
-- carries integration-specific state.
-- ============================================================================

CREATE TABLE integrations.google_calendar_tokens (
    user_id             UUID PRIMARY KEY,
    access_token_enc    TEXT NOT NULL,
    refresh_token_enc   TEXT NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    scope               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE integrations.calendar_webhook_events (
    id                  UUID PRIMARY KEY,
    provider            VARCHAR(20) NOT NULL,
    external_event_id   VARCHAR(200) NOT NULL,
    project_id          UUID,
    payload             JSONB NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'applied', 'dead')),
    error               TEXT,
    UNIQUE (provider, external_event_id)
);
CREATE INDEX ix_integrations_calendar_webhooks_status
    ON integrations.calendar_webhook_events (status, received_at DESC);

CREATE TABLE integrations.outbox (LIKE identity.outbox INCLUDING ALL);
