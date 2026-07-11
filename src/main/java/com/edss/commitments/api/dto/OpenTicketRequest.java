package com.edss.commitments.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenTicketRequest(
        UUID projectId,
        @NotBlank @Size(max = 200) String subject,
        @Size(max = 8000) String description,
        @Pattern(regexp = "^(low|normal|high|urgent)$") String priority,
        boolean isMaintenance) {}
