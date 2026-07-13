package com.edss.careers.domain;

public enum JobApplicationStatus {
    NEW("new"),
    REVIEWING("reviewing"),
    CONTACTED("contacted"),
    REJECTED("rejected"),
    HIRED("hired");

    private final String wire;

    JobApplicationStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static JobApplicationStatus ofWire(String wire) {
        for (JobApplicationStatus s : values()) {
            if (s.wire.equals(wire)) return s;
        }
        throw new IllegalArgumentException("Unknown JobApplicationStatus: " + wire);
    }
}
