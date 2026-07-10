package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResetPhaseRequest(
        @NotBlank
                @Pattern(
                        regexp =
                                "^(discussion|contract_pending|contract_signed|onboarding_scheduled|onboarding_complete|advance_invoiced|assets_pending|assets_received|in_progress|client_review|final_submission|final_invoiced|maintenance|closed)$")
                String targetPhase,
        @NotBlank @Size(max = 2000) String note) {}
