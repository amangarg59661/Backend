package com.edss.knowledge.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DownloadUrlResponse(URI url) {}
