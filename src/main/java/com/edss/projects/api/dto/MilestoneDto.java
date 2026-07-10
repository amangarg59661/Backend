package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneDto(
        UUID id,
        UUID projectId,
        int ordinal,
        String title,
        String description,
        Long amountMinor,
        String status,
        Instant dueAt,
        Instant submittedAt,
        Instant approvedAt) {}
