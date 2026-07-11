package com.edss.integrations.calendar.api;

import com.edss.integrations.calendar.CalendarWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public calendar webhook receivers. Bypass Spring Security — HMAC verified
 * inside the concrete {@link com.edss.integrations.calendar.CalendarWebhookClient}
 * impl.
 */
@RestController
@RequestMapping("/api/v1/webhooks/calendar")
@Tag(name = "calendar-webhooks", description = "HMAC-verified booking callbacks.")
public class CalendarWebhookController {

    private final CalendarWebhookService webhookService;

    public CalendarWebhookController(CalendarWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(@PathVariable String provider, HttpServletRequest request)
            throws IOException {
        String rawBody =
                new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = signatureFor(provider, request);
        webhookService.handle(provider, signature, rawBody);
        return ResponseEntity.ok().build();
    }

    private static String signatureFor(String provider, HttpServletRequest request) {
        return switch (provider) {
            case "calcom" -> request.getHeader("X-Cal-Signature-256");
            case "calendly" -> request.getHeader("Calendly-Webhook-Signature");
            default -> null;
        };
    }
}
