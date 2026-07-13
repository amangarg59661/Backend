package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobApplicationReviewRequest(
        @NotBlank
                @Pattern(regexp = "^(new|reviewing|contacted|rejected|hired)$")
                String status,
        @Size(max = 4000) String note) {}
