package com.edss.projects.domain;

public enum ReviewVerdict {
    APPROVED("approved"),
    CHANGES_REQUESTED("changes_requested"),
    REJECTED("rejected");

    private final String wire;

    ReviewVerdict(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static ReviewVerdict ofWire(String v) {
        for (ReviewVerdict s : values()) {
            if (s.wire.equals(v)) return s;
        }
        throw new IllegalArgumentException("Unknown verdict: " + v);
    }
}
