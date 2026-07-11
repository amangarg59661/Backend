package com.edss.commitments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "commitments", name = "ticket_messages")
public class TicketMessage {

    @Id private UUID id;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    private String body;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected TicketMessage() {}

    public TicketMessage(UUID id, UUID ticketId, UUID authorUserId, String body, Instant createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
