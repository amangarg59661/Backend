package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * When {@code needsTwoFa=true} the client must complete the 2FA challenge
 * before receiving a session. {@code availableMethods} enumerates the
 * methods the user has enrolled ({@code totp}, {@code whatsapp_otp},
 * {@code backup_code}); the client picks any one, sends the code to
 * {@code /auth/2fa/verify}.
 *
 * <p>M-01: {@code @JsonProperty("needs_2fa")} overrides the global
 * SNAKE_CASE mapping (which would otherwise produce {@code needs_two_fa})
 * to match the frontend Zod discriminator locked in
 * {@code packages/validation/src/api.ts}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        @JsonProperty("needs_2fa") boolean needsTwoFa,
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
