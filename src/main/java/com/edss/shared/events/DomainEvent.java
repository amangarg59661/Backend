package com.edss.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for all domain events. Every event carries the fields required by the
 * versioned envelope so a future broker-based transport can serialize it
 * without additional annotations.
 */
public interface DomainEvent {

    UUID eventId();

    /** Snake-case dotted name, e.g. {@code identity.user_logged_in}. */
    String eventType();

    /** Bumped whenever the payload shape changes in a breaking way. */
    int eventVersion();

    Instant occurredAt();

    String aggregateType();

    UUID aggregateId();

    default String producerModule() {
        return eventType().split("\\.")[0];
    }
}
