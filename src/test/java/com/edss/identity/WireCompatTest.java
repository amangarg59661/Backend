package com.edss.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.edss.identity.api.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

/**
 * Proves that the request DTOs tolerate unknown fields — the forward-compat
 * rule that lets the frontend evolve without breaking the backend.
 */
class WireCompatTest {

    private final ObjectMapper mapper =
            new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void loginRequestIgnoresUnknownFields() throws Exception {
        String bodyWithFutureField =
                "{\"email\":\"a@b.com\",\"password\":\"x\",\"remember_me\":false,"
                        + "\"future_field\":\"whatever\"}";

        LoginRequest req = mapper.readValue(bodyWithFutureField, LoginRequest.class);

        assertThat(req.email()).isEqualTo("a@b.com");
        assertThat(req.password()).isEqualTo("x");
    }
}
