package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.outbox")
public record OutboxProperties(long relayIntervalMs, int batchSize) {}
