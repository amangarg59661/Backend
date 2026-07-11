package com.edss.identity;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edss.testsupport.PostgresRedisContainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;

/**
 * Runs only when Docker is available. On CI set {@code -Ddocker.available=true}
 * (see .github/workflows/ci.yml). Locally the test skips silently unless the
 * same flag is passed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "docker.available", matches = "true")
class AuthControllerIT {

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new IllegalStateException(
                    "docker.available=true but Docker daemon is not reachable.");
        }
        PostgresRedisContainers.registerProperties(registry);
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void adminLoginReturnsAccessAndRefreshTokens() throws Exception {
        String body =
                json.writeValueAsString(
                        Map.of(
                                "email",
                                "admin@edss.local",
                                "password",
                                "TestAdmin@Password123!",
                                "remember_me",
                                false));

        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.needs_2fa").value(false))
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("admin@edss.local"))
                .andExpect(jsonPath("$.user.primaryRole").value("staff"));
    }

    @Test
    void wrongPasswordReturnsInvalidCredentials() throws Exception {
        String body =
                json.writeValueAsString(
                        Map.of(
                                "email",
                                "admin@edss.local",
                                "password",
                                "wrong",
                                "remember_me",
                                false));

        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
