package com.edss.shared.api;

import jakarta.servlet.http.HttpServletRequest;

/** Cross-controller helpers for HTTP request extraction. */
public final class HttpRequests {

    private HttpRequests() {}

    /**
     * Best-effort client IP. Prefers the first entry in {@code X-Forwarded-For}
     * when present, falls back to {@link HttpServletRequest#getRemoteAddr()}.
     * Trusts the header — deploy behind a proxy that scrubs client-supplied
     * XFF before this backend if you rely on IP for security decisions.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
