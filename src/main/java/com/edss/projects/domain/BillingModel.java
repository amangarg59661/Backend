package com.edss.projects.domain;

public enum BillingModel {
    PER_MILESTONE("per_milestone"),
    WHOLE_PROJECT("whole_project");

    private final String wire;

    BillingModel(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static BillingModel ofWire(String v) {
        for (BillingModel m : values()) {
            if (m.wire.equals(v)) return m;
        }
        throw new IllegalArgumentException("Unknown billing model: " + v);
    }
}
