package com.edss.shared.api;

import org.springframework.http.HttpStatus;

/**
 * Mirrors the frontend {@code ApiErrorCode} union in {@code
 * packages/types/src/index.ts}. Adding a value here must be matched on the
 * frontend; renaming or removing a value is a breaking change.
 */
public enum ApiErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_TOTP(HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED),
    CSRF_MISMATCH(HttpStatus.FORBIDDEN),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR),
    NETWORK_ERROR(HttpStatus.BAD_GATEWAY),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
