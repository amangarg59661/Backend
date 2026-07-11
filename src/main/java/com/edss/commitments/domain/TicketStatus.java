package com.edss.commitments.domain;

public enum TicketStatus {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    WAITING("waiting"),
    RESOLVED("resolved"),
    CLOSED("closed");

    private final String wire;

    TicketStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static TicketStatus ofWire(String v) {
        for (TicketStatus s : values()) {
            if (s.wire.equals(v)) return s;
        }
        throw new IllegalArgumentException("Unknown ticket status: " + v);
    }
}
