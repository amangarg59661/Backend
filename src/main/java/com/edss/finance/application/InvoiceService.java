package com.edss.finance.application;

import com.edss.finance.domain.Invoice;
import com.edss.finance.domain.events.FinanceEvents;
import com.edss.finance.infrastructure.InvoiceRepository;
import com.edss.integrations.payments.PaymentGateway;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoices;
    private final Map<String, PaymentGateway> gatewaysById;
    private final OutboxWriter outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong(1);

    public InvoiceService(
            InvoiceRepository invoices,
            List<PaymentGateway> gateways,
            OutboxWriter outbox,
            ObjectMapper objectMapper,
            Clock clock) {
        this.invoices = invoices;
        this.gatewaysById = new java.util.HashMap<>();
        for (PaymentGateway gw : gateways) {
            this.gatewaysById.put(gw.providerId(), gw);
        }
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Invoice issue(NewInvoice spec, String successUrl, String cancelUrl) {
        PaymentGateway gateway = gatewaysById.get(spec.provider());
        if (gateway == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Provider " + spec.provider() + " is not enabled on this deployment.");
        }

        Instant now = clock.instant();
        UUID invoiceId = UUID.randomUUID();
        String number = "INV-" + Instant.now().getEpochSecond() + "-" + sequence.getAndIncrement();
        String lineItemsJson;
        try {
            lineItemsJson = objectMapper.writeValueAsString(spec.lineItems());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialise line items", ex);
        }

        Invoice invoice =
                new Invoice(
                        invoiceId,
                        spec.clientUserId(),
                        spec.projectId(),
                        spec.milestoneId(),
                        number,
                        spec.amountMinor(),
                        spec.currency(),
                        spec.provider(),
                        lineItemsJson,
                        now,
                        spec.dueAt(),
                        now);
        invoices.save(invoice);

        PaymentGateway.CreatePaymentResult providerResult =
                gateway.createPayment(
                        new PaymentGateway.CreatePaymentRequest(
                                number,
                                spec.amountMinor(),
                                spec.currency(),
                                spec.description() == null ? number : spec.description(),
                                spec.clientEmail(),
                                successUrl,
                                cancelUrl));
        invoice.attachProviderPayment(
                providerResult.providerPaymentIntentId(), providerResult.paymentLink());

        outbox.append(
                "finance",
                new FinanceEvents.InvoiceIssued(
                        UUID.randomUUID(),
                        now,
                        invoiceId,
                        spec.clientUserId(),
                        spec.projectId(),
                        spec.milestoneId(),
                        spec.amountMinor(),
                        spec.currency(),
                        spec.provider()),
                Map.of(
                        "invoice_id", invoiceId,
                        "client_user_id", spec.clientUserId(),
                        "amount_minor", spec.amountMinor(),
                        "currency", spec.currency(),
                        "provider", spec.provider(),
                        "payment_link",
                        providerResult.paymentLink() == null ? "" : providerResult.paymentLink()));
        log.info("Issued invoice {} via {}", number, spec.provider());
        return invoice;
    }

    public Invoice markPaidManually(UUID invoiceId, UUID actorUserId) {
        Invoice invoice = fetch(invoiceId);
        if (!"manual".equals(invoice.getProvider())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Only manual invoices can be marked paid; use the provider webhook.");
        }
        markPaidInternal(invoice, "manual");
        return invoice;
    }

    public Invoice void_(UUID invoiceId, UUID actorUserId) {
        Invoice invoice = fetch(invoiceId);
        Instant now = clock.instant();
        invoice.markVoided(now);
        outbox.append(
                "finance",
                new FinanceEvents.InvoiceVoided(UUID.randomUUID(), now, invoiceId),
                Map.of("invoice_id", invoiceId, "actor_user_id", actorUserId));
        return invoice;
    }

    public void applyProviderPaid(String provider, String providerPaymentIntentId) {
        Optional<Invoice> match = invoices.findByProviderPaymentIntentId(providerPaymentIntentId);
        if (match.isEmpty()) {
            log.warn("Webhook paid event for unknown intent {} ({})", providerPaymentIntentId, provider);
            return;
        }
        markPaidInternal(match.get(), provider);
    }

    private void markPaidInternal(Invoice invoice, String provider) {
        Instant now = clock.instant();
        boolean wasPaid = "paid".equals(invoice.getStatus());
        invoice.markPaid(now);
        if (!wasPaid) {
            outbox.append(
                    "finance",
                    new FinanceEvents.InvoicePaid(
                            UUID.randomUUID(),
                            now,
                            invoice.getId(),
                            invoice.getClientUserId(),
                            invoice.getAmountMinor(),
                            invoice.getCurrency(),
                            provider),
                    Map.of(
                            "invoice_id", invoice.getId(),
                            "client_user_id", invoice.getClientUserId(),
                            "amount_minor", invoice.getAmountMinor(),
                            "currency", invoice.getCurrency(),
                            "provider", provider));
        }
    }

    @Transactional(readOnly = true)
    public Invoice fetch(UUID invoiceId) {
        return invoices.findById(invoiceId)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Invoice not found."));
    }

    @Transactional(readOnly = true)
    public List<Invoice> list(UUID actorUserId, boolean isStaff, int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        return isStaff
                ? invoices.findAllByOrderByCreatedAtDesc(lim)
                : invoices.findByClientUserIdOrderByCreatedAtDesc(actorUserId, lim);
    }

    public record NewInvoice(
            UUID clientUserId,
            String clientEmail,
            UUID projectId,
            UUID milestoneId,
            long amountMinor,
            String currency,
            String provider,
            String description,
            Instant dueAt,
            List<LineItem> lineItems) {}

    public record LineItem(String description, long amountMinor) {}
}
