package com.edss.projects.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvancePhaseRequest(@Size(max = 2000) String note) {}
