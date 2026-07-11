package com.edss.commitments.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class CommitmentsEvents {

    private CommitmentsEvents() {}

    public record TicketOpened(
            UUID eventId,
            Instant occurredAt,
            UUID ticketId,
            UUID raisedByUserId,
            UUID projectId,
            String priority,
            boolean isMaintenance)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "commitments.ticket_opened";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "ticket";
        }

        @Override
        public UUID aggregateId() {
            return ticketId;
        }
    }

    public record TicketReplied(
            UUID eventId, Instant occurredAt, UUID ticketId, UUID messageId, UUID authorUserId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "commitments.ticket_replied";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "ticket";
        }

        @Override
        public UUID aggregateId() {
            return ticketId;
        }
    }

    public record TicketStatusChanged(
            UUID eventId,
            Instant occurredAt,
            UUID ticketId,
            String fromStatus,
            String toStatus,
            UUID actorUserId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "commitments.ticket_status_changed";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "ticket";
        }

        @Override
        public UUID aggregateId() {
            return ticketId;
        }
    }
}
