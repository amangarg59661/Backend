package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneReviewDto(
        UUID id,
        UUID milestoneId,
        String verdict,
        String comment,
        UUID reviewedByUserId,
        Instant reviewedAt) {}
