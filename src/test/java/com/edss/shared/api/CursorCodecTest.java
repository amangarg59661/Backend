package com.edss.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Test
    void roundTripsCreatedAtAndId() {
        Instant createdAt = Instant.parse("2024-01-02T03:04:05.006Z");
        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");

        String cursor = CursorCodec.encode(createdAt, id);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);

        assertThat(decoded).isNotNull();
        assertThat(decoded.createdAt()).isEqualTo(createdAt);
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void nullOrBlankDecodesToNull() {
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode("")).isNull();
    }

    @Test
    void garbageCursorRaisesValidationFailed() {
        assertThatThrownBy(() -> CursorCodec.decode("not-a-cursor"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ApiErrorCode.VALIDATION_FAILED);
    }
}
