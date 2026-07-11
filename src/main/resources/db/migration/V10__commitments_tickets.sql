-- ============================================================================
-- V10__commitments_tickets.sql
-- Ticket threads + maintenance flag. Existing commitments.tickets grows
-- assignee + is_maintenance; new ticket_messages holds the conversation
-- log; commitments.outbox exists per module convention.
-- ============================================================================

ALTER TABLE commitments.tickets
    ADD COLUMN IF NOT EXISTS assignee_user_id UUID,
    ADD COLUMN IF NOT EXISTS is_maintenance BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE commitments.tickets ALTER COLUMN is_maintenance DROP DEFAULT;

CREATE INDEX IF NOT EXISTS ix_commitments_tickets_assignee
    ON commitments.tickets (assignee_user_id, updated_at DESC)
    WHERE assignee_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_commitments_tickets_project_status
    ON commitments.tickets (project_id, status, updated_at DESC)
    WHERE project_id IS NOT NULL;

CREATE TABLE commitments.ticket_messages (
    id                  UUID PRIMARY KEY,
    ticket_id           UUID NOT NULL REFERENCES commitments.tickets(id) ON DELETE CASCADE,
    author_user_id      UUID NOT NULL,
    body                TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_commitments_ticket_messages_ticket
    ON commitments.ticket_messages (ticket_id, created_at DESC);
