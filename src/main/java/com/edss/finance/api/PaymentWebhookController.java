package com.edss.finance.api;

import com.edss.finance.application.PaymentWebhookService;
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
 * Public webhook endpoints for payment providers. Bypass Spring Security auth
 * — request bodies are HMAC-verified by the concrete
 * {@link com.edss.integrations.payments.PaymentGateway} impl.
 */
@RestController
@RequestMapping("/api/v1/webhooks/payments")
@Tag(name = "payment-webhooks", description = "HMAC-verified provider callbacks.")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    public PaymentWebhookController(PaymentWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(
            @PathVariable String provider, HttpServletRequest request) throws IOException {
        String rawBody =
                new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = signatureHeaderFor(provider, request);
        webhookService.handle(provider, signature, rawBody);
        return ResponseEntity.ok().build();
    }

    private static String signatureHeaderFor(String provider, HttpServletRequest request) {
        return switch (provider) {
            case "stripe" -> request.getHeader("Stripe-Signature");
            case "razorpay" -> request.getHeader("X-Razorpay-Signature");
            default -> null;
        };
    }
}
