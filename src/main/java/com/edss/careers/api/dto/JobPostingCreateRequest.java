package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobPostingCreateRequest(
        @NotBlank
                @Size(max = 120)
                @Pattern(
                        regexp = "^[a-z0-9](-?[a-z0-9])*$",
                        message = "Slug must be lower-case alphanumerics separated by single hyphens.")
                String slug,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 80) String team,
        @NotBlank @Size(max = 200) String location,
        @NotBlank @Size(max = 40) String employmentType,
        @Size(max = 60) String commitment,
        @NotBlank String summary,
        List<@NotBlank String> responsibilities,
        List<@NotBlank String> requirements,
        Long salaryRangeMin,
        Long salaryRangeMax,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO-4217 code.")
                String currency) {}
