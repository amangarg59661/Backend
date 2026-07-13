package com.edss.shared.events;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * O-04: readiness check for the outbox relays. Reports DOWN when any per-module
 * schema has an unpublished row older than {@link #STALE_THRESHOLD} — the
 * relays are either wedged or the DB is refusing them locks. Kubernetes /
 * ECS uses {@code /actuator/health/readiness} to pull the pod out of rotation.
 */
@Component("outbox")
public class OutboxHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OutboxHealthIndicator.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    private static final List<String> SCHEMAS =
            List.of(
                    "identity",
                    "projects",
                    "finance",
                    "commitments",
                    "knowledge",
                    "notifications",
                    "relationship",
                    "careers");

    private final JdbcTemplate jdbc;

    public OutboxHealthIndicator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        Health.Builder builder = Health.up();
        boolean anyStale = false;
        for (String schema : SCHEMAS) {
            Long stale = null;
            try {
                stale =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM "
                                        + schema
                                        + ".outbox WHERE published_at IS NULL AND occurred_at < ?",
                                Long.class,
                                java.sql.Timestamp.from(cutoff));
            } catch (RuntimeException ex) {
                // Missing schema (dev without a certain module) or DB blip.
                // Don't flip the whole indicator to DOWN on one skipped schema.
                log.debug("Health probe skipped for schema {}: {}", schema, ex.getMessage());
                continue;
            }
            builder.withDetail(schema + ".outbox_stale", stale);
            if (stale != null && stale > 0) {
                anyStale = true;
            }
        }
        return anyStale ? builder.down().build() : builder.build();
    }
}
