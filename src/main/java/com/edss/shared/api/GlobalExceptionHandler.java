package com.edss.shared.api;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorBody> handleApi(ApiException ex) {
        ApiErrorBody body = new ApiErrorBody(ex.code(), ex.getMessage(), ex.details());
        return ResponseEntity.status(ex.code().status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> details = new HashMap<>();
        details.put("fields", fields);
        return respond(ApiErrorCode.VALIDATION_FAILED, "Validation failed.", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorBody> handleConstraint(ConstraintViolationException ex) {
        return respond(ApiErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorBody> handleAuth(AuthenticationException ex) {
        if (ex instanceof AuthenticationCredentialsNotFoundException) {
            return respond(ApiErrorCode.SESSION_EXPIRED, "Session expired.", null);
        }
        return respond(ApiErrorCode.INVALID_CREDENTIALS, "Authentication failed.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorBody> handleAccessDenied(AccessDeniedException ex) {
        return respond(ApiErrorCode.FORBIDDEN, "Forbidden.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorBody> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(ApiErrorCode.SERVER_ERROR, "Internal server error.", null);
    }

    private ResponseEntity<ApiErrorBody> respond(
            ApiErrorCode code, String message, Map<String, Object> details) {
        return ResponseEntity.status(code.status()).body(new ApiErrorBody(code, message, details));
    }
}
