package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.payments")
public record PaymentProperties(Stripe stripe, Razorpay razorpay) {

    public record Stripe(String secretKey, String webhookSecret) {}

    public record Razorpay(String keyId, String keySecret, String webhookSecret) {}
}
