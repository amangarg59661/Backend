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
 * Cal.com booking webhook receiver. Signature is a hex HMAC-SHA256 of the
 * raw body under the shared secret, delivered in {@code X-Cal-Signature-256}.
 * Payload references the project via a {@code metadata.project_id} field the
 * frontend sets when creating the booking link.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.calendar.provider",
        havingValue = "calcom")
public class CalcomClient implements CalendarWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(CalcomClient.class);

    private final ObjectMapper objectMapper;
    private final CalendarProperties.Calcom config;

    public CalcomClient(CalendarProperties properties, ObjectMapper objectMapper) {
        this.config = properties.calcom();
        this.objectMapper = objectMapper;
        if (config == null
                || config.webhookSecret() == null
                || config.webhookSecret().isBlank()) {
            throw new IllegalStateException(
                    "CALCOM_WEBHOOK_SECRET must be set when calendar.provider=calcom.");
        }
    }

    @Override
    public String providerId() {
        return "calcom";
    }

    @Override
    public Result verify(String signatureHeader, String rawBody) {
        String expected = HmacSignatures.hmacSha256Hex(config.webhookSecret(), rawBody);
        if (!HmacSignatures.constantTimeEquals(expected, signatureHeader)) {
            return Result.invalid("Signature mismatch.");
        }
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String triggerEvent = payload.path("triggerEvent").asText();
            JsonNode data = payload.path("payload");
            String uid = data.path("uid").asText();
            String start = data.path("startTime").asText();
            String meetingUrl = data.path("meetingUrl").asText(null);
            UUID projectId = null;
            JsonNode metadata = data.path("metadata");
            if (metadata.has("project_id")) {
                try {
                    projectId = UUID.fromString(metadata.path("project_id").asText());
                } catch (IllegalArgumentException ignored) {
                    // project_id malformed; keep null so downstream logs it
                }
            }
            Instant scheduledAt = start.isBlank() ? null : Instant.parse(start);
            return new Result(true, uid, triggerEvent, projectId, scheduledAt, meetingUrl, null);
        } catch (Exception ex) {
            log.warn("Cal.com payload parse failed", ex);
            return Result.invalid("Payload parse error: " + ex.getMessage());
        }
    }
}
