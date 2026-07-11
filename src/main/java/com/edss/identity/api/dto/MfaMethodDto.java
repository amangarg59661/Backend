package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MfaMethodDto(
        UUID id,
        String methodType,
        boolean enabled,
        String phoneE164,
        Instant enrolledAt,
        Long remainingCount) {}
