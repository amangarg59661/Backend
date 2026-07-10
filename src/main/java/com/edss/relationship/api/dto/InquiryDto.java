package com.edss.relationship.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InquiryDto(
        UUID id,
        String name,
        String email,
        String phone,
        String service,
        String message,
        String status,
        String source,
        UUID convertedToUserId,
        Instant submittedAt,
        Instant reviewedAt,
        UUID reviewedByUserId) {}
