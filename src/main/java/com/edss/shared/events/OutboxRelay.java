package com.edss.shared.events;

import com.edss.shared.config.OutboxProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-module poller that drains its schema's {@code outbox} table and hands
 * events to {@link ApplicationEventPublisher} for in-process listeners. Swap
 * the {@link #publish(EventEnvelope)} sink for a broker producer when the
 * monolith is split — no change is needed on any writer.
 *
 * <p>Uses {@code SELECT ... FOR UPDATE SKIP LOCKED} so multiple instances can
 * poll safely without duplicate delivery within a batch.</p>
 */
public abstract class OutboxRelay {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String schema;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;
    private final OutboxProperties properties;

    protected OutboxRelay(
            String schema,
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            ApplicationEventPublisher publisher,
            OutboxProperties properties) {
        this.schema = schema;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void drainOnce() {
        String selectSql =
                "SELECT id, event_type, event_version, aggregate_type, aggregate_id, payload,"
                        + " occurred_at, trace_id FROM "
                        + schema
                        + ".outbox WHERE published_at IS NULL ORDER BY occurred_at ASC LIMIT ? FOR"
                        + " UPDATE SKIP LOCKED";
        List<EventEnvelope> batch =
                jdbc.query(
                        selectSql,
                        (rs, i) -> {
                            String json = rs.getString("payload");
                            JsonNode payload;
                            try {
                                payload = objectMapper.readTree(json);
                            } catch (Exception ex) {
                                throw new IllegalStateException(
                                        "Corrupt outbox payload for id " + rs.getObject("id"), ex);
                            }
                            return new EventEnvelope(
                                    rs.getObject("id", UUID.class),
                                    rs.getString("event_type"),
                                    rs.getInt("event_version"),
                                    rs.getTimestamp("occurred_at").toInstant(),
                                    rs.getString("aggregate_type"),
                                    rs.getObject("aggregate_id", UUID.class),
                                    schema,
                                    rs.getString("trace_id"),
                                    payload);
                        },
                        properties.batchSize());

        if (batch.isEmpty()) {
            return;
        }

        // Mark the whole batch published BEFORE handing to listeners. If a
        // sync listener throws, downstream side effects will re-run on the
        // next tick — but at-least-once with idempotent listeners beats the
        // previous behaviour where a mid-batch failure re-emitted every
        // already-published event (real duplicate emails / WhatsApp / DB rows).
        // The SELECT FOR UPDATE row locks are released only at tx commit, so
        // no other poller can grab these rows in the interim.
        String updateSql =
                "UPDATE " + schema + ".outbox SET published_at = ? WHERE id = ANY (?)";
        UUID[] ids = batch.stream().map(EventEnvelope::eventId).toArray(UUID[]::new);
        Instant now = Instant.now();
        jdbc.update(updateSql, Timestamp.from(now), ids);

        for (EventEnvelope envelope : batch) {
            try {
                publish(envelope);
            } catch (RuntimeException ex) {
                log.warn("Listener failed for event {}; will not retry", envelope.eventId(), ex);
            }
        }
    }

    protected void publish(EventEnvelope envelope) {
        publisher.publishEvent(envelope);
    }
}
