package com.edss.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.edss.shared.events.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationCopyResolverTest {

    private final NotificationCopyResolver resolver = new NotificationCopyResolver();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void inquiryConvertedIncludesInviteToken() throws Exception {
        var copy = resolver.resolve(envelope("relationship.inquiry_converted",
                mapper.readTree("{\"invite_token\":\"secret-tok\",\"name\":\"Ana\"}")));
        assertThat(copy.title()).contains("Welcome");
        assertThat(copy.body()).contains("secret-tok");
        assertThat(copy.body()).contains("Ana");
    }

    @Test
    void passwordResetIncludesToken() throws Exception {
        var copy = resolver.resolve(envelope("identity.password_reset_requested",
                mapper.readTree("{\"reset_token\":\"rst-xyz\"}")));
        assertThat(copy.body()).contains("rst-xyz");
    }

    @Test
    void invoicePaidUsesSuccessSeverity() throws Exception {
        var copy = resolver.resolve(envelope("finance.invoice_paid", mapper.readTree("{}")));
        assertThat(copy.severity()).isEqualTo("success");
    }

    @Test
    void unknownEventFallsBackToGeneric() throws Exception {
        var copy = resolver.resolve(envelope("something.brand_new", mapper.readTree("{}")));
        assertThat(copy.title()).isEqualTo("Notification");
        assertThat(copy.body()).isEqualTo("something.brand_new");
    }

    private static EventEnvelope envelope(String eventType, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                Instant.now(),
                "test",
                UUID.randomUUID(),
                "test",
                "trace",
                payload);
    }
}
