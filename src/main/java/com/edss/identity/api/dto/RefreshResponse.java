package com.edss.identity.api.dto;

import java.util.List;

public record RefreshResponse(
        String accessToken,
        long accessTokenExp,
        String refreshToken,
        List<String> permissions,
        String sessionId) {}
