package com.edss.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.security")
public record SecurityProperties(Jwt jwt, Cors cors, RateLimit rateLimit) {

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}

    public record Cors(List<String> allowedOrigins) {}

    public record RateLimit(
            int loginPerEmailPerWindow, int loginPerIpPerWindow, Duration window) {}
}
