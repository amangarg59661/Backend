package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobApplicationSubmitRequest(
        @NotBlank @Size(max = 200) String applicantName,
        @NotBlank @Email @Size(max = 320) String applicantEmail,
        @Size(max = 40) String applicantPhone,
        @URL @Size(max = 2000) String resumeUrl,
        @NotBlank @Size(min = 20, max = 20_000) String coverLetter) {}
