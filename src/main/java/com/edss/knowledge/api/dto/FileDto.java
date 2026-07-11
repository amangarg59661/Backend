package com.edss.knowledge.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileDto(
        UUID id,
        UUID ownerUserId,
        String name,
        long sizeBytes,
        String mimeType,
        String kind,
        UUID projectId,
        UUID milestoneId,
        Instant createdAt) {}
