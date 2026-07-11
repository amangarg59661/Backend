package com.edss.integrations.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HmacSignaturesTest {

    @Test
    void hmacIsDeterministicPerSecretAndBody() {
        String a = HmacSignatures.hmacSha256Hex("shh", "payload");
        String b = HmacSignatures.hmacSha256Hex("shh", "payload");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void hmacDiffersAcrossSecrets() {
        assertThat(HmacSignatures.hmacSha256Hex("one", "x"))
                .isNotEqualTo(HmacSignatures.hmacSha256Hex("two", "x"));
    }

    @Test
    void hmacDiffersAcrossBodies() {
        assertThat(HmacSignatures.hmacSha256Hex("k", "a"))
                .isNotEqualTo(HmacSignatures.hmacSha256Hex("k", "b"));
    }

    @Test
    void constantTimeEqualsMatchesForEqual() {
        assertThat(HmacSignatures.constantTimeEquals("abc123", "abc123")).isTrue();
    }

    @Test
    void constantTimeEqualsFailsForDifferent() {
        assertThat(HmacSignatures.constantTimeEquals("abc", "abd")).isFalse();
    }

    @Test
    void constantTimeEqualsFailsForNull() {
        assertThat(HmacSignatures.constantTimeEquals(null, "x")).isFalse();
        assertThat(HmacSignatures.constantTimeEquals("x", null)).isFalse();
    }
}
