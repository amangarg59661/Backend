package com.edss.integrations.payments;

import com.edss.shared.config.PaymentProperties;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Razorpay Payment Link gateway. Creates a hosted payment page whose URL
 * becomes the invoice's payment_link. Webhook covers
 * {@code payment_link.paid} + {@code payment.captured} events.
 */
@Component
@ConditionalOnProperty(name = "edss.features.payments.razorpay-enabled", havingValue = "true")
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final RazorpayClient client;
    private final PaymentProperties.Razorpay config;

    public RazorpayGateway(PaymentProperties properties) throws RazorpayException {
        this.config = properties.razorpay();
        if (config == null
                || config.keyId() == null
                || config.keyId().isBlank()
                || config.keySecret() == null
                || config.keySecret().isBlank()) {
            throw new IllegalStateException(
                    "RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET must be set when"
                            + " payments.razorpay-enabled=true.");
        }
        this.client = new RazorpayClient(config.keyId(), config.keySecret());
    }

    @Override
    public String providerId() {
        return "razorpay";
    }

    @Override
    public CreatePaymentResult createPayment(CreatePaymentRequest request) {
        try {
            JSONObject body = new JSONObject();
            body.put("amount", request.amountMinor());
            body.put("currency", request.currency());
            body.put("description", request.description());
            body.put("reference_id", request.invoiceNumber());
            JSONObject customer = new JSONObject();
            customer.put("email", request.clientEmail());
            body.put("customer", customer);
            body.put("callback_url", request.successUrl());
            body.put("callback_method", "get");
            JSONObject notify = new JSONObject();
            notify.put("email", true);
            notify.put("sms", false);
            body.put("notify", notify);

            PaymentLink link = client.paymentLink.create(body);
            String id = link.get("id");
            String shortUrl = link.get("short_url");
            return new CreatePaymentResult(id, shortUrl);
        } catch (RazorpayException ex) {
            log.error("Razorpay payment-link creation failed", ex);
            throw new PaymentProviderException("Razorpay payment creation failed.", ex);
        }
    }

    @Override
    public WebhookVerification verify(String signatureHeader, String rawBody) {
        try {
            boolean valid = Utils.verifyWebhookSignature(rawBody, signatureHeader, config.webhookSecret());
            if (!valid) {
                return WebhookVerification.invalid("Signature verification failed.");
            }
            JSONObject payload = new JSONObject(rawBody);
            String eventId =
                    payload.optString("id", payload.optString("event", "") + ":" + payload.optString("created_at", ""));
            String eventType = payload.optString("event");
            String intentId = null;
            JSONObject payloadObj = payload.optJSONObject("payload");
            if (payloadObj != null) {
                JSONObject paymentLink = payloadObj.optJSONObject("payment_link");
                if (paymentLink != null && paymentLink.has("entity")) {
                    intentId = paymentLink.getJSONObject("entity").optString("id", null);
                }
                if (intentId == null) {
                    JSONObject payment = payloadObj.optJSONObject("payment");
                    if (payment != null && payment.has("entity")) {
                        intentId = payment.getJSONObject("entity").optString("payment_link_id", null);
                    }
                }
            }
            return new WebhookVerification(true, eventId, eventType, intentId, null);
        } catch (RazorpayException ex) {
            return WebhookVerification.invalid("Signature verification error: " + ex.getMessage());
        }
    }
}
