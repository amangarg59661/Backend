package com.edss.projects.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-module command port for scheduling an onboarding call on a project.
 * Used by calendar webhook receivers so no external caller touches
 * {@code projects.application} directly.
 */
public interface OnboardingCallScheduler {

    void schedule(
            UUID projectId,
            String provider,
            Instant scheduledAt,
            String meetingUrl,
            String externalRef);
}
