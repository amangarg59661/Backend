package com.edss.knowledge.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PresignUploadRequest(
        @NotBlank @Size(max = 300) String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._+/*-]{1,200}$") String contentType,
        @Min(1) long sizeBytes,
        @NotBlank
                @Pattern(regexp = "^(project_asset|milestone_deliverable|general|contract|avatar)$")
                String kind,
        UUID projectId,
        UUID milestoneId) {}
