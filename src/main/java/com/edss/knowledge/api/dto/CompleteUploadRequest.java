package com.edss.knowledge.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompleteUploadRequest(@Min(1) long actualSize) {}
