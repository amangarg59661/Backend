package com.edss.notifications.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationDto(
        UUID id,
        UUID userId,
        String severity,
        String title,
        String body,
        boolean read,
        Instant createdAt,
        String href) {}
