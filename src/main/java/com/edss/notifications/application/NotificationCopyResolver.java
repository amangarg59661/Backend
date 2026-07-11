package com.edss.notifications.application;

import com.edss.notifications.application.NotificationChannel.NotificationCopy;
import com.edss.shared.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Central lookup for human-readable copy per event type. Kept explicit so a
 * missing entry surfaces at review time rather than shipping a stringly-typed
 * default. Frontend uses the {@code event_type} field on the notification row
 * to render richer, localised copy — this fallback copy is for email + WhatsApp.
 */
@Component
public class NotificationCopyResolver {

    private interface Renderer {
        NotificationCopy render(EventEnvelope envelope);
    }

    private final Map<String, Renderer> renderers = new java.util.HashMap<>();

    public NotificationCopyResolver() {
        renderers.put(
                "identity.user_registered",
                env -> info("Account created", "Your EDSS account is ready."));
        renderers.put(
                "relationship.inquiry_converted",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    String token = p.path("invite_token").asText();
                    String name = p.path("name").asText("");
                    return info(
                            "Welcome to EDSS — set your password",
                            "Hi "
                                    + name
                                    + ",\n\nSet your password using this token:\n\n"
                                    + token
                                    + "\n\nThis invite expires in 7 days.");
                });
        renderers.put(
                "projects.project_created",
                env -> info("Project created", "A new project has been created for you."));
        renderers.put(
                "projects.phase_transitioned",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Project phase updated",
                            "Project moved to phase: " + p.path("to_phase").asText());
                });
        renderers.put(
                "projects.milestone_submitted",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Milestone submitted",
                            "Milestone #" + p.path("ordinal").asInt() + " is ready for review.");
                });
        renderers.put(
                "projects.milestone_reviewed",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Milestone reviewed",
                            "Verdict: " + p.path("verdict").asText());
                });
        renderers.put(
                "projects.contract_uploaded",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Contract uploaded",
                            "A " + p.path("kind").asText() + " contract is available.");
                });
        renderers.put(
                "projects.onboarding_scheduled",
                env ->
                        info(
                                "Onboarding call scheduled",
                                "Your onboarding call is on the calendar."));
        renderers.put(
                "projects.maintenance_started",
                env -> info("Maintenance started", "The maintenance window has begun."));
        renderers.put(
                "projects.project_closed",
                env -> success("Project closed", "The project has been closed."));
        renderers.put(
                "finance.invoice_issued",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Invoice issued",
                            "Amount: "
                                    + amount(p.path("amount_minor").asLong())
                                    + " "
                                    + p.path("currency").asText());
                });
        renderers.put(
                "finance.invoice_paid",
                env -> success("Payment received", "We have received your payment. Thank you."));
        renderers.put(
                "finance.invoice_voided",
                env -> warning("Invoice voided", "An invoice was voided."));
        renderers.put(
                "commitments.ticket_opened",
                env -> info("Ticket opened", "A new ticket has been opened."));
        renderers.put(
                "commitments.ticket_replied",
                env -> info("Ticket reply", "There is a new reply on your ticket."));
        renderers.put(
                "commitments.ticket_status_changed",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Ticket status changed",
                            "Status: " + p.path("to_status").asText());
                });
        renderers.put(
                "identity.password_reset_requested",
                env -> {
                    JsonNode p = (JsonNode) env.payload();
                    String token = p.path("reset_token").asText();
                    return info(
                            "Reset your EDSS password",
                            "Use this token to reset your password:\n\n"
                                    + token
                                    + "\n\nExpires in 30 minutes.");
                });
    }

    public NotificationCopy resolve(EventEnvelope envelope) {
        Renderer renderer = renderers.get(envelope.eventType());
        if (renderer == null) {
            return info("Notification", envelope.eventType());
        }
        return renderer.render(envelope);
    }

    private static NotificationCopy info(String title, String body) {
        return new NotificationCopy("info", title, body, null);
    }

    private static NotificationCopy success(String title, String body) {
        return new NotificationCopy("success", title, body, null);
    }

    private static NotificationCopy warning(String title, String body) {
        return new NotificationCopy("warning", title, body, null);
    }

    private static String amount(long minor) {
        return String.format("%.2f", minor / 100.0);
    }
}
