package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsappEnrollStartRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phoneE164) {}
