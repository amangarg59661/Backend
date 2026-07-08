package com.edss.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Wire shape matching the frontend {@code paginationSchema} — {@code items},
 * opaque {@code cursor}, {@code has_more} flag. Cursor is a base64url blob
 * produced by {@link CursorCodec}; clients treat it as opaque.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaginatedResponse<T>(List<T> items, String cursor, boolean hasMore) {

    public static <T> PaginatedResponse<T> empty() {
        return new PaginatedResponse<>(List.of(), null, false);
    }
}
