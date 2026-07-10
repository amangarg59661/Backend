package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login body. {@code trustedDeviceToken} is optional — when set and valid it
 * bypasses the 2FA challenge on this specific device.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        boolean rememberMe,
        String trustedDeviceToken) {}
