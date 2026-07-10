package com.edss.relationship.domain;

/**
 * Lifecycle of a public inquiry. {@code new} → {@code in_review} while staff
 * triages, then either {@code converted} (client account created) or
 * {@code rejected}.
 */
public enum InquiryStatus {
    NEW("new"),
    IN_REVIEW("in_review"),
    CONVERTED("converted"),
    REJECTED("rejected");

    private final String wire;

    InquiryStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static InquiryStatus ofWire(String v) {
        for (InquiryStatus s : values()) {
            if (s.wire.equals(v)) return s;
        }
        throw new IllegalArgumentException("Unknown inquiry status: " + v);
    }
}
