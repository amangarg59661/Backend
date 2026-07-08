package com.edss.shared.api;

import java.util.Map;

/**
 * Thrown by application code to signal an API error with a specific error
 * code. The {@link GlobalExceptionHandler} translates it into an HTTP response
 * matching the frontend {@code apiErrorSchema} envelope.
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final Map<String, Object> details;

    public ApiException(ApiErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ApiErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ApiErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
