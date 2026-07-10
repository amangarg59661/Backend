package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingCallUpsertRequest(
        @NotBlank @Pattern(regexp = "^(calcom|calendly|manual)$") String provider,
        Instant scheduledAt,
        @Size(max = 500) String meetingUrl,
        @Size(max = 200) String externalRef) {}
