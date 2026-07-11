package com.edss.commitments.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketMessageDto(
        UUID id, UUID ticketId, UUID authorUserId, String body, Instant createdAt) {}
