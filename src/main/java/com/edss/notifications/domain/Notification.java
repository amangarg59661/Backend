package com.edss.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "notifications", name = "notifications")
public class Notification {

    @Id private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private String severity;

    private String title;

    private String body;

    private String href;

    private boolean read;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "channel_mask")
    private int channelMask;

    @Column(name = "event_type")
    private String eventType;

    protected Notification() {}

    public Notification(
            UUID id,
            UUID userId,
            String severity,
            String title,
            String body,
            String href,
            Instant createdAt) {
        this(id, userId, severity, title, body, href, createdAt, 0, null);
    }

    public Notification(
            UUID id,
            UUID userId,
            String severity,
            String title,
            String body,
            String href,
            Instant createdAt,
            int channelMask,
            String eventType) {
        this.id = id;
        this.userId = userId;
        this.severity = severity;
        this.title = title;
        this.body = body;
        this.href = href;
        this.read = false;
        this.createdAt = createdAt;
        this.channelMask = channelMask;
        this.eventType = eventType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getHref() {
        return href;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getChannelMask() {
        return channelMask;
    }

    public String getEventType() {
        return eventType;
    }

    public void markRead() {
        this.read = true;
    }

    public void addChannel(int bit) {
        this.channelMask |= bit;
    }
}
