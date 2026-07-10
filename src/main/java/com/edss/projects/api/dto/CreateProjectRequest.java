package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateProjectRequest(
        @NotNull UUID clientUserId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotBlank @Pattern(regexp = "^(per_milestone|whole_project)$") String billingModel,
        @Min(0) Integer maintenanceDurationDays,
        @Min(0) Long totalAmountMinor,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Valid List<CreateMilestone> milestones) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateMilestone(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Min(0) Long amountMinor,
            Instant dueAt) {}
}
