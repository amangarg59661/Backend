package com.edss.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvoiceDto(
        UUID id,
        UUID clientUserId,
        UUID projectId,
        UUID milestoneId,
        String number,
        long amountMinor,
        String currency,
        String status,
        String provider,
        String paymentLink,
        Instant issuedAt,
        Instant dueAt,
        Instant paidAt,
        Instant createdAt) {}
