package com.edss.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape used by the outbox relay when handing an event to the in-process
 * bus (and, later, to a broker producer). Keeping the envelope as a record
 * separate from the domain event lets the domain event evolve without changing
 * the delivery contract.
 *
 * <p><strong>Idempotency contract:</strong> the outbox relay marks rows
 * {@code published_at} inside the SELECT-FOR-UPDATE transaction and only then
 * fires listeners. If a listener throws, the row stays marked and the failure
 * is logged (and captured by Sentry). Modulith's own event publication log
 * additionally replays incomplete listeners on restart. Both paths mean the
 * same envelope may arrive at a listener more than once. Every listener must
 * therefore dedupe on {@link #eventId()} — the UUID is stable across all
 * replays of a given business event. Concrete example: {@code
 * InAppChannel.deliver} relies on {@code notifications.notifications UNIQUE
 * (user_id, event_id)} and swallows {@code DataIntegrityViolationException}
 * on duplicate delivery.</p>
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
