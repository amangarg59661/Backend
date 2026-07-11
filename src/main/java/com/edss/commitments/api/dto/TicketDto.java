package com.edss.commitments.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketDto(
        UUID id,
        UUID raisedByUserId,
        UUID projectId,
        String subject,
        String description,
        String priority,
        String status,
        UUID assigneeUserId,
        boolean isMaintenance,
        Instant createdAt,
        Instant updatedAt) {}
