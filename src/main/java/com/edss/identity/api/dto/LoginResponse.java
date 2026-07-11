package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * When {@code needsTwoFa=true} the client must complete the 2FA challenge
 * before receiving a session. {@code availableMethods} enumerates the
 * methods the user has enrolled ({@code totp}, {@code whatsapp_otp},
 * {@code backup_code}); the client picks any one, sends the code to
 * {@code /auth/2fa/verify}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        boolean needsTwoFa,
        String twoFaChallengeId,
        List<String> availableMethods,
        String accessToken,
        Long accessTokenExp,
        String refreshToken,
        UserDto user,
        List<String> permissions,
        String sessionId,
        String trustedDeviceToken) {

    public static LoginResponse challenge(String challengeId, List<String> availableMethods) {
        return new LoginResponse(
                true, challengeId, availableMethods, null, null, null, null, null, null, null);
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
