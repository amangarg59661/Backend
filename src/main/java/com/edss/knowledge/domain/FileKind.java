package com.edss.knowledge.domain;

public enum FileKind {
    PROJECT_ASSET("project_asset"),
    MILESTONE_DELIVERABLE("milestone_deliverable"),
    GENERAL("general"),
    CONTRACT("contract"),
    AVATAR("avatar");

    private final String wire;

    FileKind(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static FileKind ofWire(String v) {
        for (FileKind k : values()) {
            if (k.wire.equals(v)) return k;
        }
        throw new IllegalArgumentException("Unknown file kind: " + v);
    }
}
