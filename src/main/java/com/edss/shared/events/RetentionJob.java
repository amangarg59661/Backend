package com.edss.shared.events;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PF-12: hourly retention sweeper. Removes rows that are no longer useful
 * and would otherwise accumulate forever:
 *
 * <ul>
 *   <li>Published outbox rows older than {@link #OUTBOX_RETENTION}. Once
 *       {@code published_at} is set the relay is done with the row and the
 *       Modulith event log has its own copy for late listeners.</li>
 *   <li>Expired password reset tokens (rows past {@code expires_at}).</li>
 *   <li>Trusted device tokens past {@code expires_at} or revoked more than
 *       30 days ago.</li>
 * </ul>
 *
 * <p>Compliance-driven retention (user profile data, invoice history) is a
 * client-owned decision and not swept here — that lives in the Wave 3
 * compliance backlog. This job only touches operational plumbing.</p>
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);
    private static final Duration OUTBOX_RETENTION = Duration.ofDays(30);
    private static final Duration REVOKED_DEVICE_RETENTION = Duration.ofDays(30);

    private static final List<String> OUTBOX_SCHEMAS =
            List.of(
                    "identity",
                    "projects",
                    "finance",
                    "commitments",
                    "knowledge",
                    "notifications",
                    "relationship");

    private final JdbcTemplate jdbc;

    public RetentionJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Hourly cadence — cheap enough to run frequently, keeps tables small. */
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void sweep() {
        Instant now = Instant.now();
        sweepOutbox(now);
        sweepPasswordResetTokens(now);
        sweepTrustedDevices(now);
    }

    private void sweepOutbox(Instant now) {
        Timestamp cutoff = Timestamp.from(now.minus(OUTBOX_RETENTION));
        for (String schema : OUTBOX_SCHEMAS) {
            try {
                int removed =
                        jdbc.update(
                                "DELETE FROM "
                                        + schema
                                        + ".outbox WHERE published_at IS NOT NULL AND published_at < ?",
                                cutoff);
                if (removed > 0) {
                    log.info("Retention sweep removed {} rows from {}.outbox", removed, schema);
                }
            } catch (RuntimeException ex) {
                log.debug("Outbox sweep skipped for {}: {}", schema, ex.getMessage());
            }
        }
    }

    private void sweepPasswordResetTokens(Instant now) {
        try {
            int removed =
                    jdbc.update(
                            "DELETE FROM identity.password_reset_tokens WHERE expires_at < ?",
                            Timestamp.from(now));
            if (removed > 0) {
                log.info("Retention sweep removed {} expired password reset tokens", removed);
            }
        } catch (RuntimeException ex) {
            log.debug("Reset-token sweep skipped: {}", ex.getMessage());
        }
    }

    private void sweepTrustedDevices(Instant now) {
        Timestamp expiredCutoff = Timestamp.from(now);
        Timestamp revokedCutoff = Timestamp.from(now.minus(REVOKED_DEVICE_RETENTION));
        try {
            int removed =
                    jdbc.update(
                            "DELETE FROM identity.trusted_devices"
                                    + " WHERE expires_at < ? OR (revoked_at IS NOT NULL AND revoked_at < ?)",
                            expiredCutoff,
                            revokedCutoff);
            if (removed > 0) {
                log.info("Retention sweep removed {} trusted device tokens", removed);
            }
        } catch (RuntimeException ex) {
            log.debug("Trusted-device sweep skipped: {}", ex.getMessage());
        }
    }
}
