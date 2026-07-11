package com.edss.integrations.messaging;

import com.edss.shared.config.MessagingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Minimal Twilio WhatsApp Business client. Sends via
 * {@code POST /2010-04-01/Accounts/{Sid}/Messages.json} with Basic auth,
 * form-urlencoded body. Returns the Twilio message SID for delivery tracking.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.messaging.whatsapp-enabled",
        havingValue = "true")
public class TwilioWhatsappClient {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsappClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final MessagingProperties.Twilio config;

    public TwilioWhatsappClient(MessagingProperties properties, ObjectMapper objectMapper) {
        this.config = properties.twilio();
        this.objectMapper = objectMapper;
        if (config == null
                || config.accountSid() == null
                || config.accountSid().isBlank()
                || config.authToken() == null
                || config.authToken().isBlank()
                || config.whatsappFrom() == null
                || config.whatsappFrom().isBlank()) {
            throw new IllegalStateException(
                    "TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM must be set when"
                            + " whatsapp-enabled=true.");
        }
    }

    /** @return Twilio message SID */
    public String send(String toE164, String body) {
        String url =
                "https://api.twilio.com/2010-04-01/Accounts/"
                        + config.accountSid()
                        + "/Messages.json";
        String form =
                "To=" + URLEncoder.encode("whatsapp:" + toE164, StandardCharsets.UTF_8)
                        + "&From=" + URLEncoder.encode("whatsapp:" + config.whatsappFrom(), StandardCharsets.UTF_8)
                        + "&Body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        String auth =
                Base64.getEncoder()
                        .encodeToString((config.accountSid() + ":" + config.authToken()).getBytes(StandardCharsets.UTF_8));
        HttpRequest req =
                HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Basic " + auth)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                        .build();
        try {
            HttpResponse<String> res =
                    http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 300) {
                throw new RuntimeException(
                        "Twilio send failed [" + res.statusCode() + "]: " + res.body());
            }
            JsonNode payload = objectMapper.readTree(res.body());
            return payload.path("sid").asText();
        } catch (Exception ex) {
            log.warn("Twilio WhatsApp send threw", ex);
            throw new RuntimeException("Twilio send failed", ex);
        }
    }
}
