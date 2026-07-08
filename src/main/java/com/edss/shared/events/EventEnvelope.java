package com.edss.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape used by the outbox relay when handing an event to the in-process
 * bus (and, later, to a broker producer). Keeping the envelope as a record
 * separate from the domain event lets the domain event evolve without changing
 * the delivery contract.
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String aggregateType,
        UUID aggregateId,
        String producerModule,
        String traceId,
        Object payload) {}
