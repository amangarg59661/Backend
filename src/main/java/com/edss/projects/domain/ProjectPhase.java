package com.edss.projects.domain;

/**
 * Sequential lifecycle phases. Forward-only via the state machine; backward
 * moves require {@code admin:project:override} and land in
 * {@code project_phase_history} with a note.
 */
public enum ProjectPhase {
    DISCUSSION("discussion"),
    CONTRACT_PENDING("contract_pending"),
    CONTRACT_SIGNED("contract_signed"),
    ONBOARDING_SCHEDULED("onboarding_scheduled"),
    ONBOARDING_COMPLETE("onboarding_complete"),
    ADVANCE_INVOICED("advance_invoiced"),
    ASSETS_PENDING("assets_pending"),
    ASSETS_RECEIVED("assets_received"),
    IN_PROGRESS("in_progress"),
    CLIENT_REVIEW("client_review"),
    FINAL_SUBMISSION("final_submission"),
    FINAL_INVOICED("final_invoiced"),
    MAINTENANCE("maintenance"),
    CLOSED("closed");

    private final String wire;

    ProjectPhase(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static ProjectPhase ofWire(String v) {
        for (ProjectPhase p : values()) {
            if (p.wire.equals(v)) return p;
        }
        throw new IllegalArgumentException("Unknown project phase: " + v);
    }
}
