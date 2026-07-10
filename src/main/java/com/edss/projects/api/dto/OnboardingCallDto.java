package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnboardingCallDto(
        UUID id,
        UUID projectId,
        String provider,
        Instant scheduledAt,
        String meetingUrl,
        String externalRef,
        String status) {}
