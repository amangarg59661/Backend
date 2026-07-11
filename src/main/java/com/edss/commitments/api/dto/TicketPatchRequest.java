package com.edss.commitments.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketPatchRequest(
        @Pattern(regexp = "^(open|in_progress|waiting|resolved|closed)$") String status,
        UUID assigneeUserId) {}
