package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** All fields optional; null means no change. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobPostingUpdateRequest(
        @Size(max = 200) String title,
        @Size(max = 80) String team,
        @Size(max = 200) String location,
        @Size(max = 40) String employmentType,
        @Size(max = 60) String commitment,
        String summary,
        List<String> responsibilities,
        List<String> requirements,
        Long salaryRangeMin,
        Long salaryRangeMax,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO-4217 code.")
                String currency) {}
