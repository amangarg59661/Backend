package com.edss.integrations.calendar;

import com.edss.shared.config.CalendarProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Calendly webhook receiver. Signature header {@code Calendly-Webhook-Signature}
 * contains {@code t=<timestamp>,v1=<sig>} where sig is HMAC-SHA256 of
 * {@code timestamp + "." + rawBody}. Project id lands via a
 * {@code questions_and_answers} entry named {@code project_id}.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.calendar.provider",
        havingValue = "calendly")
public class CalendlyClient implements CalendarWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(CalendlyClient.class);

    private final ObjectMapper objectMapper;
    private final CalendarProperties.Calendly config;

    public CalendlyClient(CalendarProperties properties, ObjectMapper objectMapper) {
        this.config = properties.calendly();
        this.objectMapper = objectMapper;
        if (config == null
                || config.webhookSecret() == null
                || config.webhookSecret().isBlank()) {
            throw new IllegalStateException(
                    "CALENDLY_WEBHOOK_SECRET must be set when calendar.provider=calendly.");
        }
    }

    @Override
    public String providerId() {
        return "calendly";
    }

    @Override
    public Result verify(String signatureHeader, String rawBody) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return Result.invalid("Missing signature header.");
        }
        String timestamp = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("t".equals(kv[0])) timestamp = kv[1];
            if ("v1".equals(kv[0])) signature = kv[1];
        }
        if (timestamp == null || signature == null) {
            return Result.invalid("Malformed signature header.");
        }
        String expected =
                HmacSignatures.hmacSha256Hex(config.webhookSecret(), timestamp + "." + rawBody);
        if (!HmacSignatures.constantTimeEquals(expected, signature)) {
            return Result.invalid("Signature mismatch.");
        }
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String eventType = payload.path("event").asText();
            JsonNode payloadData = payload.path("payload");
            String uri = payloadData.path("uri").asText();
            String start = payloadData.path("scheduled_event").path("start_time").asText();
            String meetingUrl =
                    payloadData
                            .path("scheduled_event")
                            .path("location")
                            .path("join_url")
                            .asText(null);
            UUID projectId = null;
            for (JsonNode qa : payloadData.path("questions_and_answers")) {
                if ("project_id".equalsIgnoreCase(qa.path("question").asText())) {
                    try {
                        projectId = UUID.fromString(qa.path("answer").asText());
                    } catch (IllegalArgumentException ignored) {
                        // malformed; downstream will surface via status=dead
                    }
                }
            }
            Instant scheduledAt = start.isBlank() ? null : Instant.parse(start);
            return new Result(true, uri, eventType, projectId, scheduledAt, meetingUrl, null);
        } catch (Exception ex) {
            log.warn("Calendly payload parse failed", ex);
            return Result.invalid("Payload parse error: " + ex.getMessage());
        }
    }
}
