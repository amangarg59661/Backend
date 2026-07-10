package com.edss.integrations.payments;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bank-transfer / offline gateway. No hosted checkout, no payment link.
 * Accountant marks the invoice paid via the {@code /invoices/{id}/mark-paid}
 * endpoint when the transfer clears.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.payments.manual-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ManualGateway implements PaymentGateway {

    @Override
    public String providerId() {
        return "manual";
    }

    @Override
    public CreatePaymentResult createPayment(CreatePaymentRequest request) {
        return new CreatePaymentResult(null, null);
    }

    @Override
    public WebhookVerification verify(String signatureHeader, String rawBody) {
        return WebhookVerification.invalid("Manual gateway does not receive webhooks.");
    }
}
