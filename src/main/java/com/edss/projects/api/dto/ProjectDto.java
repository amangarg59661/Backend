package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDto(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        String status,
        String phase,
        String billingModel,
        Integer maintenanceDurationDays,
        Instant maintenanceStartsAt,
        Instant maintenanceEndsAt,
        Long totalAmountMinor,
        String currency,
        Instant createdAt,
        Instant updatedAt) {}
