package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Verify a 2FA challenge. {@code method} names which enrolled method the
 * client is completing (returned in {@code LoginResponse.availableMethods}).
 * {@code code} format depends on the method: 6 digits for TOTP + WhatsApp
 * OTP, 10-char upper-case alphanumeric for backup codes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwoFaVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "^(totp|whatsapp_otp|backup_code)$") String method,
        @NotBlank String code,
        boolean rememberDevice) {}
