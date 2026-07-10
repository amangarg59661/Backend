package com.edss.relationship.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Staff-side status transition. Allowed targets: {@code in_review} or
 * {@code rejected}. Conversion has its own endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InquiryStatusUpdateRequest(
        @NotBlank @Pattern(regexp = "^(in_review|rejected)$") String status) {}
