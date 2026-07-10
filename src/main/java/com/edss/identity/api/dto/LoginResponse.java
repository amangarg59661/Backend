package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Matches the discriminated union at {@code loginResponseSchema} — when {@code
 * needsTwoFa=true} the client should immediately prompt for the 6-digit code
 * using {@code twoFaChallengeId}. When false all session fields are populated.
 * {@code trustedDeviceToken} is only present when the caller opted into
 * "remember this device" during a 2FA verify.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        boolean needsTwoFa,
        String twoFaChallengeId,
        String accessToken,
        Long accessTokenExp,
        String refreshToken,
        UserDto user,
        List<String> permissions,
        String sessionId,
        String trustedDeviceToken) {

    public static LoginResponse challenge(String challengeId) {
        return new LoginResponse(true, challengeId, null, null, null, null, null, null, null);
    }

    public static LoginResponse full(
            String accessToken,
            long accessTokenExp,
            String refreshToken,
            UserDto user,
            List<String> permissions,
            String sessionId,
            String trustedDeviceToken) {
        return new LoginResponse(
                false,
                null,
                accessToken,
                accessTokenExp,
                refreshToken,
                user,
                permissions,
                sessionId,
                trustedDeviceToken);
    }
}
