package com.edss.integrations.payments;

/**
 * Provider port for issuing invoice payments. Each concrete gateway is
 * {@code @ConditionalOnProperty} on its feature flag AND its credentials —
 * both must be present. Callers select a gateway by
 * {@link #providerId()} at invoice creation.
 */
public interface PaymentGateway {

    String providerId();

    CreatePaymentResult createPayment(CreatePaymentRequest request);

    /**
     * Verify a webhook body against the provider's HMAC signature. Returns the
     * canonical {@code external_event_id} for idempotency dedup.
     */
    WebhookVerification verify(String signatureHeader, String rawBody);

    record CreatePaymentRequest(
            String invoiceNumber,
            long amountMinor,
            String currency,
            String description,
            String clientEmail,
            String successUrl,
            String cancelUrl) {}

    record CreatePaymentResult(String providerPaymentIntentId, String paymentLink) {}

    record WebhookVerification(
            boolean valid,
            String externalEventId,
            String eventType,
            String providerPaymentIntentId,
            String reason) {

        public static WebhookVerification invalid(String reason) {
            return new WebhookVerification(false, null, null, null, reason);
        }
    }
}
