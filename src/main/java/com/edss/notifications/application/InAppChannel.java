package com.edss.notifications.application;

import com.edss.notifications.domain.Notification;
import com.edss.notifications.infrastructure.NotificationRepository;
import com.edss.shared.events.EventEnvelope;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Persists an in-app notification row + pushes it to the recipient's
 * WebSocket queue at {@code /user/{userId}/queue/notifications}.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.notifications.channels.in-app",
        havingValue = "true",
        matchIfMissing = true)
public class InAppChannel implements NotificationChannel {

    public static final int BIT = 2;
    private static final Logger log = LoggerFactory.getLogger(InAppChannel.class);

    private final NotificationRepository notifications;
    private final SimpMessagingTemplate messaging;
    private final Clock clock;

    public InAppChannel(
            NotificationRepository notifications,
            SimpMessagingTemplate messaging,
            Clock clock) {
        this.notifications = notifications;
        this.messaging = messaging;
        this.clock = clock;
    }

    @Override
    public String channelId() {
        return "in_app";
    }

    @Override
    public int bitMask() {
        return BIT;
    }

    @Override
    public void deliver(NotificationRecipient recipient, EventEnvelope envelope, NotificationCopy copy) {
        Notification row =
                new Notification(
                        UUID.randomUUID(),
                        recipient.userId(),
                        copy.severity(),
                        copy.title(),
                        copy.body(),
                        copy.href(),
                        clock.instant(),
                        BIT,
                        envelope.eventType(),
                        envelope.eventId());
        try {
            notifications.save(row);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Outbox replay for an already-delivered event. Idempotency
            // contract: no additional row, no WebSocket push. Log at debug.
            log.debug(
                    "Duplicate in-app delivery skipped for user={} event_id={}",
                    recipient.userId(),
                    envelope.eventId());
            return;
        }
        try {
            messaging.convertAndSendToUser(
                    recipient.userId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "id", row.getId(),
                            "user_id", row.getUserId(),
                            "severity", row.getSeverity(),
                            "title", row.getTitle(),
                            "body", row.getBody(),
                            "read", row.isRead(),
                            "created_at", row.getCreatedAt(),
                            "event_type", envelope.eventType()));
        } catch (RuntimeException ex) {
            log.warn("WebSocket push failed for user {}: {}", recipient.userId(), ex.getMessage());
        }
    }
}
