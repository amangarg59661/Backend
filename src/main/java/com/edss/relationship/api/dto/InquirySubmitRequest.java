package com.edss.relationship.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InquirySubmitRequest(
        @NotBlank @Size(max = 200) String name,
        @Email @NotBlank @Size(max = 320) String email,
        @Size(max = 40) String phone,
        @Size(max = 200) String service,
        @Size(max = 4000) String message,
        @Size(max = 80) String source) {}
