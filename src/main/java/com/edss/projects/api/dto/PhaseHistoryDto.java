package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhaseHistoryDto(
        UUID id,
        UUID projectId,
        String fromPhase,
        String toPhase,
        UUID actorUserId,
        String note,
        Instant transitionedAt) {}
