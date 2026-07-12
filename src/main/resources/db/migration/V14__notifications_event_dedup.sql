-- ============================================================================
-- V14__notifications_event_dedup.sql
-- Adds event_id + UNIQUE (user_id, event_id) so InAppChannel.deliver becomes
-- idempotent on outbox at-least-once replays. Fixes A-01 + A-16 from the
-- audit remediation branch.
-- ============================================================================

ALTER TABLE notifications.notifications
    ADD COLUMN IF NOT EXISTS event_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS ix_notifications_user_event_dedup
    ON notifications.notifications (user_id, event_id)
    WHERE event_id IS NOT NULL;
