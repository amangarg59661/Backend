package com.edss.shared.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque cursor encoding for keyset pagination. Encodes a
 * {@code (created_at, id)} tuple as base64url. The wire shape is intentionally
 * opaque so the encoded form can change without breaking clients.
 */
public final class CursorCodec {

    private static final String SEP = "|";
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(Instant createdAt, UUID id) {
        String raw = createdAt.toString() + SEP + id;
        return ENC.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(DEC.decode(cursor), StandardCharsets.UTF_8);
            int sepIdx = raw.indexOf(SEP);
            if (sepIdx <= 0) {
                throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Invalid cursor.");
            }
            return new Cursor(
                    Instant.parse(raw.substring(0, sepIdx)),
                    UUID.fromString(raw.substring(sepIdx + 1)));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Invalid cursor.");
        }
    }

    public record Cursor(Instant createdAt, UUID id) {}
}
