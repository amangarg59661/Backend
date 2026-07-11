-- ============================================================================
-- V11__notifications_channels.sql
-- Multi-channel delivery. channel_mask records which channels the event
-- was routed to. whatsapp_deliveries tracks Twilio message state per user.
-- ============================================================================

ALTER TABLE notifications.notifications
    ADD COLUMN IF NOT EXISTS channel_mask INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(120);

ALTER TABLE notifications.notifications ALTER COLUMN channel_mask DROP DEFAULT;

CREATE INDEX IF NOT EXISTS ix_notifications_user_read
    ON notifications.notifications (user_id, read, created_at DESC);

CREATE TABLE notifications.whatsapp_deliveries (
    id                  UUID PRIMARY KEY,
    notification_id     UUID REFERENCES notifications.notifications(id) ON DELETE CASCADE,
    phone_e164          VARCHAR(20) NOT NULL,
    twilio_sid          VARCHAR(120),
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'sent', 'delivered', 'failed')),
    delivered_at        TIMESTAMPTZ,
    error               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_notifications_whatsapp_deliveries_notification
    ON notifications.whatsapp_deliveries (notification_id, created_at DESC);
