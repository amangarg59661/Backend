package com.edss.relationship.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Public inquiry lifecycle events emitted through the relationship outbox. */
public final class RelationshipEvents {

    private RelationshipEvents() {}

    public record InquirySubmitted(
            UUID eventId, Instant occurredAt, UUID inquiryId, String email, String service)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "relationship.inquiry_submitted";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "inquiry";
        }

        @Override
        public UUID aggregateId() {
            return inquiryId;
        }
    }

    /**
     * C-2: fired alongside InquirySubmitted specifically so the
     * notifications module can route a "thanks for reaching out" email
     * back to the submitter without conflating the staff-triage signal.
     */
    public record InquiryAcknowledged(
            UUID eventId, Instant occurredAt, UUID inquiryId, String email, String name)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "relationship.inquiry_acknowledged";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "inquiry";
        }

        @Override
        public UUID aggregateId() {
            return inquiryId;
        }
    }

    public record InquiryConverted(
            UUID eventId,
            Instant occurredAt,
            UUID inquiryId,
            UUID newUserId,
            String email,
            String name,
            String inviteToken)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "relationship.inquiry_converted";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "inquiry";
        }

        @Override
        public UUID aggregateId() {
            return inquiryId;
        }
    }
}
