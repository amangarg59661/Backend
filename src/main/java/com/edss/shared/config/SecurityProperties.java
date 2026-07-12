package com.edss.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.security")
public record SecurityProperties(Jwt jwt, Cors cors, RateLimit rateLimit, Secrets secrets) {

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}

    public record Cors(List<String> allowedOrigins) {}

    public record RateLimit(
            int loginPerEmailPerWindow, int loginPerIpPerWindow, Duration window) {}

    public record Secrets(
            /** Legacy single-key config; if set, treated as key-id "v1". Prefer keys + activeKid. */
            String encryptionKey,
            /**
             * Named AES-GCM keys keyed by key-id. Enables rotation: add a new
             * kid, flip {@code activeKid}, keep the old kid available for
             * decrypting historical ciphertext until it is re-encrypted.
             * Values are base64-encoded 32-byte keys. S-10.
             */
            java.util.Map<String, String> keys,
            /** Which kid new encryptions use. Must be present in {@code keys}. */
            String activeKid) {}
}
