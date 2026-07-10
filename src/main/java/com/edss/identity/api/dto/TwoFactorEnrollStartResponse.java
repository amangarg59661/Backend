package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response for {@code POST /users/me/2fa/enroll/start}. Frontend renders the
 * QR PNG (data URI) inside an authenticator-app picker. Both {@code secret}
 * and {@code otpauth_uri} are supplied so a user without a scanner can key
 * the secret in by hand.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TwoFactorEnrollStartResponse(
        String secret, String otpauthUri, String qrCodePngBase64) {}
