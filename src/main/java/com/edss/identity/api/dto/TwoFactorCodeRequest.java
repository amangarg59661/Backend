package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Verify a 6-digit TOTP code during 2FA enrollment.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwoFactorCodeRequest(@NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}
