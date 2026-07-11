package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.messaging")
public record MessagingProperties(Twilio twilio) {

    public record Twilio(String accountSid, String authToken, String whatsappFrom) {}
}
