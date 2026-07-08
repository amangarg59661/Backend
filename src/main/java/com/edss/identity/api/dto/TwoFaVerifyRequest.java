package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwoFaVerifyRequest(
        @NotBlank String challengeId,
        @Pattern(regexp = "^\\d{6}$") String code,
        boolean rememberDevice) {}
