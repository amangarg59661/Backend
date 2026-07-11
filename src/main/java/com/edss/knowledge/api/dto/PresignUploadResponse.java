package com.edss.knowledge.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PresignUploadResponse(String uploadId, URI presignedUrl, Instant expiresAt) {}
