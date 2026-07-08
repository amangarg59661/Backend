package com.edss.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Error envelope matching the frontend {@code apiErrorSchema}. {@code details}
 * is omitted from the JSON when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorBody(ApiErrorCode code, String message, Map<String, Object> details) {}
