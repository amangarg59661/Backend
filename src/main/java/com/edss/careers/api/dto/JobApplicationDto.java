package com.edss.careers.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobApplicationDto(
        UUID id,
        UUID jobPostingId,
        String applicantName,
        String applicantEmail,
        String applicantPhone,
        String resumeUrl,
        String coverLetter,
        String status,
        String reviewerNote,
        Instant submittedAt,
        Instant reviewedAt,
        UUID reviewedByUserId) {}
