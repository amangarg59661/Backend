-- ============================================================================
-- V8__finance_payments.sql
-- Invoice provider dispatch (stripe / razorpay / manual), payment webhook
-- idempotency store, per-project + per-milestone linkage, line items as
-- JSONB, finance outbox.
-- ============================================================================

ALTER TABLE finance.invoices
    ADD COLUMN IF NOT EXISTS project_id UUID,
    ADD COLUMN IF NOT EXISTS milestone_id UUID,
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'manual'
        CHECK (provider IN ('stripe', 'razorpay', 'manual')),
    ADD COLUMN IF NOT EXISTS provider_payment_intent_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_link TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS line_items JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE finance.invoices
    ALTER COLUMN provider DROP DEFAULT,
    ALTER COLUMN line_items DROP DEFAULT;

CREATE INDEX IF NOT EXISTS ix_finance_invoices_project
    ON finance.invoices (project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_finance_invoices_milestone
    ON finance.invoices (milestone_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_finance_invoices_provider_intent
    ON finance.invoices (provider_payment_intent_id)
    WHERE provider_payment_intent_id IS NOT NULL;

-- Webhook dedup + audit. UNIQUE on (provider, external_event_id) prevents
-- double-apply when a provider retries a delivery.
CREATE TABLE finance.payment_webhook_events (
    id                  UUID PRIMARY KEY,
    provider            VARCHAR(20) NOT NULL,
    external_event_id   VARCHAR(200) NOT NULL,
    invoice_id          UUID,
    payload             JSONB NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'applied', 'dead')),
    error               TEXT,
    UNIQUE (provider, external_event_id)
);
CREATE INDEX ix_finance_payment_webhooks_status_received
    ON finance.payment_webhook_events (status, received_at DESC);

-- Finance outbox mirrored from identity.outbox.  Already created in V1;
-- IF NOT EXISTS keeps this migration idempotent on fresh databases.
CREATE TABLE IF NOT EXISTS finance.outbox (LIKE identity.outbox INCLUDING ALL);
