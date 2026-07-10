package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registers a contract PDF against a project. {@code storageKey} refers to a
 * file already uploaded via the knowledge module's presigned-URL flow;
 * {@code sha256} is the client-computed digest of the exact bytes uploaded.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterContractRequest(
        @NotBlank @Pattern(regexp = "^(unsigned|signed)$") String kind,
        @NotBlank @Size(max = 512) String storageKey,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String sha256) {}
