package com.edss.projects.domain;

public enum MilestoneStatus {
    PLANNED("planned"),
    IN_PROGRESS("in_progress"),
    SUBMITTED("submitted"),
    CHANGES_REQUESTED("changes_requested"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String wire;

    MilestoneStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static MilestoneStatus ofWire(String v) {
        for (MilestoneStatus s : values()) {
            if (s.wire.equals(v)) return s;
        }
        throw new IllegalArgumentException("Unknown milestone status: " + v);
    }
}
