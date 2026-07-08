package com.edss.identity.infrastructure;

import com.edss.shared.config.OutboxProperties;
import com.edss.shared.events.OutboxRelay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdentityOutboxRelay extends OutboxRelay {

    public IdentityOutboxRelay(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            ApplicationEventPublisher publisher,
            OutboxProperties properties) {
        super("identity", jdbc, objectMapper, publisher, properties);
    }

    @Scheduled(fixedDelayString = "${edss.outbox.relay-interval-ms:250}")
    void relayScheduled() {
        drainOnce();
    }
}
