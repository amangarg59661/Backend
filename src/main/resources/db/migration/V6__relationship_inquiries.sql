-- ============================================================================
-- V6__relationship_inquiries.sql
-- Public inquiry funnel: anonymous form → staff triage → convert to client.
-- Converted inquiries link to the identity.users row that was created.
-- ============================================================================

CREATE TABLE relationship.inquiries (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(200) NOT NULL,
    email                   VARCHAR(320) NOT NULL,
    phone                   VARCHAR(40),
    service                 VARCHAR(200),
    message                 TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'new'
                                CHECK (status IN ('new', 'in_review', 'converted', 'rejected')),
    source                  VARCHAR(80),
    converted_to_user_id    UUID,
    submitted_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at             TIMESTAMPTZ,
    reviewed_by_user_id     UUID,
    extensions              JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_relationship_inquiries_status_submitted
    ON relationship.inquiries (status, submitted_at DESC);
CREATE INDEX ix_relationship_inquiries_email
    ON relationship.inquiries (email);

CREATE TABLE relationship.outbox (LIKE identity.outbox INCLUDING ALL);
