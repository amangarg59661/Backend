package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.calendar")
public record CalendarProperties(Calcom calcom, Calendly calendly, Google google) {

    public record Calcom(String apiKey, String webhookSecret) {}

    public record Calendly(String apiKey, String webhookSecret) {}

    public record Google(String oauthClientId, String oauthClientSecret, String redirectUri) {}
}
