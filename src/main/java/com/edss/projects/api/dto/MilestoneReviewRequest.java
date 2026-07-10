package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MilestoneReviewRequest(
        @NotBlank @Pattern(regexp = "^(approved|changes_requested|rejected)$") String verdict,
        @Size(max = 4000) String comment) {}
