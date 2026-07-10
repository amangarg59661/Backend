package com.edss.relationship.infrastructure;

import com.edss.shared.config.OutboxProperties;
import com.edss.shared.events.OutboxRelay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.storage.outbox-relay",
        havingValue = "true",
        matchIfMissing = true)
public class RelationshipOutboxRelay extends OutboxRelay {

    public RelationshipOutboxRelay(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            ApplicationEventPublisher publisher,
            OutboxProperties properties) {
        super("relationship", jdbc, objectMapper, publisher, properties);
    }

    @Scheduled(fixedDelayString = "${edss.outbox.relay-interval-ms:250}")
    void relayScheduled() {
        drainOnce();
    }
}
