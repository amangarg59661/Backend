package com.edss.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * User wire shape. Matches the frontend {@code userSchema} exactly — those
 * fields are camelCase because Zod defines them that way, so they override
 * the global snake_case Jackson strategy via {@link JsonProperty}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
        UUID id,
        String email,
        String name,
        @JsonProperty("avatarUrl") String avatarUrl,
        @JsonProperty("primaryRole") String primaryRole,
        @JsonProperty("hasBothRoles") boolean hasBothRoles,
        @JsonProperty("createdAt") Instant createdAt) {}
