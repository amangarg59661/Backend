package com.edss.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edss.shared.config.SecurityProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecretCipherTest {

    private static final String KEY_V1 = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";
    private static final String KEY_V2 = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";

    private static final SecurityProperties WITH_LEGACY_KEY =
            new SecurityProperties(
                    new SecurityProperties.Jwt("k", Duration.ofMinutes(15), Duration.ofDays(30)),
                    new SecurityProperties.Cors(List.of("http://localhost:3001")),
                    new SecurityProperties.RateLimit(5, 20, Duration.ofMinutes(15)),
                    new SecurityProperties.Secrets(KEY_V1, null, null));

    private final SecretCipher cipher = new SecretCipher(WITH_LEGACY_KEY);

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
                        WITH_LEGACY_KEY.jwt(),
                        WITH_LEGACY_KEY.cors(),
                        WITH_LEGACY_KEY.rateLimit(),
                        new SecurityProperties.Secrets(null, null, null));
        assertThatThrownBy(() -> new SecretCipher(noKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void encryptedPayloadCarriesActiveKidEnvelope() {
        SecurityProperties multi =
                new SecurityProperties(
                        WITH_LEGACY_KEY.jwt(),
                        WITH_LEGACY_KEY.cors(),
                        WITH_LEGACY_KEY.rateLimit(),
                        new SecurityProperties.Secrets(null, Map.of("v2", KEY_V2), "v2"));
        SecretCipher c = new SecretCipher(multi);
        String ciphertext = c.encrypt("payload");
        assertThat(ciphertext).startsWith("v1:v2:");
    }

    @Test
    void rotationDecryptsCiphertextsFromEitherKey() {
        // Encrypt with legacy-only config …
        String legacyCiphertext = cipher.encrypt("secret");

        // … then swap to a rotated config carrying both kids.
        SecurityProperties rotated =
                new SecurityProperties(
                        WITH_LEGACY_KEY.jwt(),
                        WITH_LEGACY_KEY.cors(),
                        WITH_LEGACY_KEY.rateLimit(),
                        new SecurityProperties.Secrets(KEY_V1, Map.of("v2", KEY_V2), "v2"));
        SecretCipher rotatedCipher = new SecretCipher(rotated);

        // Old ciphertext still decrypts via the retained legacy key.
        assertThat(rotatedCipher.decrypt(legacyCiphertext)).isEqualTo("secret");
        // New writes carry the new kid.
        String newCiphertext = rotatedCipher.encrypt("secret");
        assertThat(newCiphertext).startsWith("v1:v2:");
        assertThat(rotatedCipher.decrypt(newCiphertext)).isEqualTo("secret");
    }

    @Test
    void rejectsUnknownKidInEnvelope() {
        SecretCipher c =
                new SecretCipher(
                        new SecurityProperties(
                                WITH_LEGACY_KEY.jwt(),
                                WITH_LEGACY_KEY.cors(),
                                WITH_LEGACY_KEY.rateLimit(),
                                new SecurityProperties.Secrets(KEY_V1, null, null)));
        String forged = "v1:nope:AAAAAAAAAAAAAAAAAAAAAAA";
        assertThatThrownBy(() -> c.decrypt(forged))
                .isInstanceOf(IllegalStateException.class);
    }
}
