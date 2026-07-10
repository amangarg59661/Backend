package com.edss.finance.application;

import com.edss.finance.domain.PaymentWebhookEvent;
import com.edss.finance.infrastructure.PaymentWebhookEventRepository;
import com.edss.integrations.payments.PaymentGateway;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies payment provider webhooks idempotently. Records the raw payload
 * under {@code (provider, external_event_id)} UNIQUE — duplicate deliveries
 * become no-ops. Successful application flips the invoice paid and emits
 * {@code finance.invoice_paid} via {@link InvoiceService}.
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);
    private static final Set<String> PAID_EVENT_TYPES =
            Set.of(
                    // Stripe
                    "checkout.session.completed",
                    "payment_intent.succeeded",
                    // Razorpay
                    "payment_link.paid",
                    "payment.captured");

    private final PaymentWebhookEventRepository events;
    private final InvoiceService invoices;
    private final Map<String, PaymentGateway> gateways;
    private final Clock clock;

    public PaymentWebhookService(
            PaymentWebhookEventRepository events,
            InvoiceService invoices,
            List<PaymentGateway> gatewayList,
            Clock clock) {
        this.events = events;
        this.invoices = invoices;
        this.gateways = new java.util.HashMap<>();
        for (PaymentGateway gw : gatewayList) {
            this.gateways.put(gw.providerId(), gw);
        }
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(String provider, String signatureHeader, String rawBody) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND, "Provider not enabled: " + provider);
        }
        PaymentGateway.WebhookVerification verification = gateway.verify(signatureHeader, rawBody);
        if (!verification.valid()) {
            log.warn("Rejected {} webhook: {}", provider, verification.reason());
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Invalid webhook signature.");
        }

        Optional<PaymentWebhookEvent> existing =
                events.findByProviderAndExternalEventId(provider, verification.externalEventId());
        if (existing.isPresent() && "applied".equals(existing.get().getStatus())) {
            log.info("Idempotent replay for {} event {}", provider, verification.externalEventId());
            return;
        }

        Instant now = clock.instant();
        PaymentWebhookEvent record =
                new PaymentWebhookEvent(
                        UUID.randomUUID(),
                        provider,
                        verification.externalEventId(),
                        null,
                        rawBody,
                        now);
        try {
            events.save(record);
        } catch (DataIntegrityViolationException ex) {
            log.info(
                    "Concurrent replay for {} event {} — already stored",
                    provider,
                    verification.externalEventId());
            return;
        }

        try {
            if (PAID_EVENT_TYPES.contains(verification.eventType())
                    && verification.providerPaymentIntentId() != null) {
                invoices.applyProviderPaid(provider, verification.providerPaymentIntentId());
            }
            record.markApplied(clock.instant());
        } catch (RuntimeException ex) {
            record.markDead(clock.instant(), ex.getMessage());
            log.error("Failed to apply {} webhook {}", provider, verification.externalEventId(), ex);
            throw ex;
        }
    }
}
