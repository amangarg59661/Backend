package com.edss.careers.domain;

public enum JobPostingStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String wire;

    JobPostingStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static JobPostingStatus ofWire(String wire) {
        for (JobPostingStatus s : values()) {
            if (s.wire.equals(wire)) return s;
        }
        throw new IllegalArgumentException("Unknown JobPostingStatus: " + wire);
    }
}
