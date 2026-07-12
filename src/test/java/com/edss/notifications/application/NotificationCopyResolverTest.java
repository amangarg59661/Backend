package com.edss.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.edss.shared.events.EventEnvelope;
import com.edss.shared.security.EphemeralSecrets;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class NotificationCopyResolverTest {

    private final StubSecrets secrets = new StubSecrets();
    private final NotificationCopyResolver resolver = new NotificationCopyResolver(secrets);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void inquiryConvertedResolvesInviteTokenFromHandle() throws Exception {
        String handle = secrets.stash("secret-tok", Duration.ofMinutes(5));
        var copy =
                resolver.resolve(
                        envelope(
                                "relationship.inquiry_converted",
                                mapper.readTree(
                                        "{\"invite_token_handle\":\""
                                                + handle
                                                + "\",\"name\":\"Ana\"}")));
        assertThat(copy.title()).contains("Welcome");
        assertThat(copy.body()).contains("secret-tok");
        assertThat(copy.body()).contains("Ana");
    }

    @Test
    void inquiryConvertedFallsBackWhenHandleExpired() throws Exception {
        var copy =
                resolver.resolve(
                        envelope(
                                "relationship.inquiry_converted",
                                mapper.readTree(
                                        "{\"invite_token_handle\":\"missing\",\"name\":\"Ana\"}")));
        assertThat(copy.body()).contains("Ask your account manager");
    }

    @Test
    void passwordResetResolvesTokenFromHandle() throws Exception {
        String handle = secrets.stash("rst-xyz", Duration.ofMinutes(5));
        var copy =
                resolver.resolve(
                        envelope(
                                "identity.password_reset_requested",
                                mapper.readTree(
                                        "{\"reset_token_handle\":\"" + handle + "\"}")));
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

    /** Minimal in-test replacement for the flag-gated Spring beans. */
    private static class StubSecrets implements EphemeralSecrets {
        private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
        private int counter = 0;

        @Override
        public synchronized String stash(String plaintext, Duration ttl) {
            String handle = "h-" + (++counter);
            store.put(handle, plaintext);
            return handle;
        }

        @Override
        public Optional<String> pop(String handle) {
            return Optional.ofNullable(store.remove(handle));
        }
    }
}
