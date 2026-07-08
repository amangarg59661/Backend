package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mirrors the frontend {@code resetPasswordSchema} password policy:
 * 12+ chars with upper, lower, digit, symbol.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank
                @Size(min = 12)
                @Pattern(
                        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                        message = "Password must include upper, lower, digit and symbol.")
                String newPassword) {}
