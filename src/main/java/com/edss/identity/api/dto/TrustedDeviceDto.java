package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrustedDeviceDto(
        UUID id,
        String userAgent,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt) {}
