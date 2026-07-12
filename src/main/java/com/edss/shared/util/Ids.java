package com.edss.shared.util;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * A-10: single entry point for aggregate id generation. Version 7 UUIDs are
 * time-sortable (48-bit Unix ms prefix) so B-tree indexes on primary keys
 * stay contiguous under insert load — random v4 fragments the index.
 *
 * <p>Every {@code new UUID(...)} at aggregate creation sites should migrate
 * to {@link #newId()} incrementally. For rows created inside a single
 * transaction where ordering does not matter, {@code UUID.randomUUID()} is
 * still acceptable but has no upside.</p>
 */
public final class Ids {

    private Ids() {}

    public static UUID newId() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
