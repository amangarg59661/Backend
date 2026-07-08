package com.edss.shared.events;

import com.edss.shared.web.RequestIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes an event to the caller module's {@code <module>.outbox} table inside
 * the caller's transaction. A per-module {@link OutboxRelay} later publishes
 * unpublished rows. This is the single seam a future Kafka producer will slot
 * into.
 */
@Component
public class OutboxWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String moduleSchema, DomainEvent event, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise event payload", e);
        }
        String sql =
                "INSERT INTO "
                        + moduleSchema
                        + ".outbox (id, event_type, event_version, aggregate_type, aggregate_id,"
                        + " payload, occurred_at, trace_id) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)";
        Instant now = event.occurredAt() != null ? event.occurredAt() : clock.instant();
        jdbc.update(
                sql,
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateType(),
                event.aggregateId(),
                payloadJson,
                java.sql.Timestamp.from(now),
                MDC.get(RequestIdFilter.MDC_KEY));
    }

    public UUID nextEventId() {
        return UUID.randomUUID();
    }
}
