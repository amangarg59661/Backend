package com.edss.finance.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class FinanceEvents {

    private FinanceEvents() {}

    public record InvoiceIssued(
            UUID eventId,
            Instant occurredAt,
            UUID invoiceId,
            UUID clientUserId,
            UUID projectId,
            UUID milestoneId,
            long amountMinor,
            String currency,
            String provider)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "finance.invoice_issued";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "invoice";
        }

        @Override
        public UUID aggregateId() {
            return invoiceId;
        }
    }

    public record InvoicePaid(
            UUID eventId,
            Instant occurredAt,
            UUID invoiceId,
            UUID clientUserId,
            long amountMinor,
            String currency,
            String provider)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "finance.invoice_paid";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "invoice";
        }

        @Override
        public UUID aggregateId() {
            return invoiceId;
        }
    }

    public record InvoiceVoided(UUID eventId, Instant occurredAt, UUID invoiceId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "finance.invoice_voided";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "invoice";
        }

        @Override
        public UUID aggregateId() {
            return invoiceId;
        }
    }
}
