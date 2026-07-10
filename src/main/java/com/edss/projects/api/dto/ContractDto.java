package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContractDto(
        UUID id,
        UUID projectId,
        String kind,
        String storageKey,
        String sha256,
        UUID uploadedByUserId,
        Instant uploadedAt) {}
