package com.edss.identity.application;

import com.edss.identity.infrastructure.WhatsappOtpStore;
import com.edss.integrations.messaging.TwilioWhatsappClient;
import java.security.SecureRandom;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Issues + verifies 6-digit OTP codes delivered via Twilio WhatsApp.
 * Twilio client is optional so this service still compiles when WhatsApp is
 * disabled — issueOtp then throws so callers surface a clear "channel off"
 * error instead of a Spring wiring failure.
 */
@Service
public class WhatsappOtpService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappOtpService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final WhatsappOtpStore store;
    private final ObjectProvider<TwilioWhatsappClient> twilio;

    public WhatsappOtpService(
            WhatsappOtpStore store, ObjectProvider<TwilioWhatsappClient> twilio) {
        this.store = store;
        this.twilio = twilio;
    }

    /** @return generated code (also stored under (userId, purpose) with 5-minute TTL) */
    public String issueOtp(UUID userId, String phoneE164, String purpose) {
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        store.put(userId, purpose, code);
        TwilioWhatsappClient client = twilio.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException(
                    "WhatsApp integration disabled — cannot deliver OTP.");
        }
        try {
            client.send(phoneE164, "Your EDSS verification code: " + code + " (valid 5 min)");
        } catch (RuntimeException ex) {
            log.warn("WhatsApp OTP send failed to {}", phoneE164, ex);
            throw ex;
        }
        return code;
    }

    public boolean verify(UUID userId, String purpose, String code) {
        return store.peek(userId, purpose).map(expected -> expected.equals(code)).orElse(false);
    }

    public void invalidate(UUID userId, String purpose) {
        store.invalidate(userId, purpose);
    }
}
