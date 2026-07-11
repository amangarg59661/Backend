package com.edss.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edss.shared.config.SecurityProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretCipherTest {

    private static final SecurityProperties WITH_KEY =
            new SecurityProperties(
                    new SecurityProperties.Jwt("k", Duration.ofMinutes(15), Duration.ofDays(30)),
                    new SecurityProperties.Cors(List.of("http://localhost:3001")),
                    new SecurityProperties.RateLimit(5, 20, Duration.ofMinutes(15)),
                    new SecurityProperties.Secrets("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="));

    private final SecretCipher cipher = new SecretCipher(WITH_KEY);

    @Test
    void roundTripsPlaintext() {
        String encrypted = cipher.encrypt("hunter2");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("hunter2");
    }

    @Test
    void everyEncryptionIsDifferent() {
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void rejectsMissingKey() {
        SecurityProperties noKey =
                new SecurityProperties(
                        WITH_KEY.jwt(),
                        WITH_KEY.cors(),
                        WITH_KEY.rateLimit(),
                        new SecurityProperties.Secrets(""));
        assertThatThrownBy(() -> new SecretCipher(noKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
