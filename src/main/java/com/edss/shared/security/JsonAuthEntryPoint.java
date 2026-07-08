package com.edss.shared.security;

import com.edss.shared.api.ApiErrorBody;
import com.edss.shared.api.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        response.setStatus(ApiErrorCode.SESSION_EXPIRED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorBody body =
                new ApiErrorBody(ApiErrorCode.SESSION_EXPIRED, "Session expired.", null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
