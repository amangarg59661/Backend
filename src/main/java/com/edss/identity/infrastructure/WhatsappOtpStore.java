package com.edss.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived OTP tokens for WhatsApp method. Same shape as
 * {@link TwoFactorChallengeStore} but keyed by (userId, purpose) so a single
 * user can have an "enrollment" OTP and a "login" OTP simultaneously.
 */
public interface WhatsappOtpStore {

    void put(UUID userId, String purpose, String code);

    Optional<String> peek(UUID userId, String purpose);

    void invalidate(UUID userId, String purpose);
}
