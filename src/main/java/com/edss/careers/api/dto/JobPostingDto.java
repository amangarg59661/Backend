package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobPostingDto(
        UUID id,
        String slug,
        String title,
        String team,
        String location,
        String employmentType,
        String commitment,
        String summary,
        List<String> responsibilities,
        List<String> requirements,
        Long salaryRangeMin,
        Long salaryRangeMax,
        String currency,
        String status,
        LocalDate postedAt,
        Instant publishedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {}
