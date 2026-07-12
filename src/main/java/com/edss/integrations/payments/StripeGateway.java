package com.edss.integrations.payments;

import com.edss.shared.config.PaymentProperties;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stripe Checkout-Session gateway. One session per invoice. Payment link is
 * the Checkout URL that the frontend redirects the client to. Webhook
 * consumes {@code checkout.session.completed} + {@code payment_intent.
 * succeeded} events to flip the invoice to paid.
 */
@Component
@ConditionalOnProperty(name = "edss.features.payments.stripe-enabled", havingValue = "true")
public class StripeGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

    private final PaymentProperties.Stripe stripeConfig;

    public StripeGateway(PaymentProperties properties) {
        this.stripeConfig = properties.stripe();
        if (stripeConfig == null
                || stripeConfig.secretKey() == null
                || stripeConfig.secretKey().isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_SECRET_KEY must be set when payments.stripe-enabled=true.");
        }
        Stripe.apiKey = stripeConfig.secretKey();
    }

    @Override
    public String providerId() {
        return "stripe";
    }

    @Override
    public CreatePaymentResult createPayment(CreatePaymentRequest request) {
        try {
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(request.successUrl())
                            .setCancelUrl(request.cancelUrl())
                            .setCustomerEmail(request.clientEmail())
                            .setClientReferenceId(request.invoiceNumber())
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency(
                                                                    request.currency()
                                                                            .toLowerCase())
                                                            .setUnitAmount(request.amountMinor())
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem
                                                                            .PriceData.ProductData
                                                                            .builder()
                                                                            .setName(
                                                                                    request
                                                                                            .description())
                                                                            .build())
                                                            .build())
                                            .build())
                            .build();
            // A-26: idempotency key keyed on invoice number so a retry after a
            // network blip does not create a duplicate Checkout Session /
            // duplicate charge on client card. Stripe stores the response for
            // 24h keyed by this string.
            RequestOptions options =
                    RequestOptions.builder()
                            .setIdempotencyKey("invoice:" + request.invoiceNumber())
                            .build();
            Session session = Session.create(params, options);
            return new CreatePaymentResult(session.getPaymentIntent(), session.getUrl());
        } catch (StripeException ex) {
            log.error("Stripe checkout session creation failed", ex);
            throw new PaymentProviderException("Stripe payment creation failed.", ex);
        }
    }

    @Override
    public WebhookVerification verify(String signatureHeader, String rawBody) {
        try {
            Event event = Webhook.constructEvent(rawBody, signatureHeader, stripeConfig.webhookSecret());
            // Non-payment events (charge.*, customer.*, invoice.* etc.) still
            // return valid=true with a null intentId — the webhook service
            // marks them applied without touching invoices. Skip the
            // deserialise cost for anything we don't care about.
            String type = event.getType();
            if (!"checkout.session.completed".equals(type)
                    && !"payment_intent.succeeded".equals(type)) {
                return new WebhookVerification(true, event.getId(), type, null, null);
            }
            String intentId = null;
            if (event.getDataObjectDeserializer() != null
                    && event.getDataObjectDeserializer().getObject().isPresent()) {
                Object obj = event.getDataObjectDeserializer().getObject().get();
                if (obj instanceof Session s) {
                    intentId = s.getPaymentIntent();
                } else if (obj instanceof com.stripe.model.PaymentIntent pi) {
                    intentId = pi.getId();
                }
            }
            return new WebhookVerification(true, event.getId(), type, intentId, null);
        } catch (SignatureVerificationException ex) {
            return WebhookVerification.invalid("Signature verification failed.");
        }
    }
}
