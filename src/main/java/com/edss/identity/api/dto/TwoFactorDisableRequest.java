package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Disable 2FA. Both fields required so a hijacked session cannot silently
 * drop the second factor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwoFactorDisableRequest(
        @NotBlank String password, @NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}
