package com.edss.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "notifications", name = "whatsapp_deliveries")
public class WhatsappDelivery {

    @Id private UUID id;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "phone_e164")
    private String phoneE164;

    @Column(name = "twilio_sid")
    private String twilioSid;

    private String status;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    private String error;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected WhatsappDelivery() {}

    public WhatsappDelivery(UUID id, UUID notificationId, String phoneE164, Instant createdAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.phoneE164 = phoneE164;
        this.status = "pending";
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public String getTwilioSid() {
        return twilioSid;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markSent(String twilioSid) {
        this.twilioSid = twilioSid;
        this.status = "sent";
    }

    public void markDelivered(Instant at) {
        this.status = "delivered";
        this.deliveredAt = at;
    }

    public void markFailed(String error) {
        this.status = "failed";
        this.error = error;
    }
}
