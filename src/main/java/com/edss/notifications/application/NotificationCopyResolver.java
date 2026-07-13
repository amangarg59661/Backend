package com.edss.notifications.application;

import com.edss.notifications.application.NotificationChannel.NotificationCopy;
import com.edss.shared.events.EventEnvelope;
import com.edss.shared.security.EphemeralSecrets;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
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
        NotificationCopy render(EventEnvelope envelope, EphemeralSecrets secrets);
    }

    private static final Map<String, Renderer> RENDERERS = buildRenderers();

    private final EphemeralSecrets ephemeralSecrets;

    public NotificationCopyResolver(EphemeralSecrets ephemeralSecrets) {
        this.ephemeralSecrets = ephemeralSecrets;
    }

    private static Map<String, Renderer> buildRenderers() {
        java.util.Map<String, Renderer> renderers = new java.util.HashMap<>();
        renderers.put(
                "identity.user_registered",
                (env, secrets) -> info("Account created", "Your EDSS account is ready."));
        renderers.put(
                "relationship.inquiry_converted",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    String handle = p.path("invite_token_handle").asText();
                    String name = p.path("name").asText("");
                    Optional<String> token = secrets.pop(handle);
                    if (token.isEmpty()) {
                        return info(
                                "Welcome to EDSS",
                                "Your account is ready. Ask your account manager for a"
                                        + " fresh invite link — the previous one expired.");
                    }
                    return info(
                            "Welcome to EDSS — set your password",
                            "Hi "
                                    + name
                                    + ",\n\nSet your password using this token:\n\n"
                                    + token.get()
                                    + "\n\nThis invite expires in 7 days.");
                });
        renderers.put(
                "projects.project_created",
                (env, secrets) -> info("Project created", "A new project has been created for you."));
        renderers.put(
                "projects.phase_transitioned",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Project phase updated",
                            "Project moved to phase: " + p.path("to_phase").asText());
                });
        renderers.put(
                "projects.milestone_submitted",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Milestone submitted",
                            "Milestone #" + p.path("ordinal").asInt() + " is ready for review.");
                });
        renderers.put(
                "projects.milestone_reviewed",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Milestone reviewed",
                            "Verdict: " + p.path("verdict").asText());
                });
        renderers.put(
                "projects.contract_uploaded",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Contract uploaded",
                            "A " + p.path("kind").asText() + " contract is available.");
                });
        renderers.put(
                "projects.onboarding_scheduled",
                (env, secrets) ->
                        info(
                                "Onboarding call scheduled",
                                "Your onboarding call is on the calendar."));
        renderers.put(
                "projects.maintenance_started",
                (env, secrets) -> info("Maintenance started", "The maintenance window has begun."));
        renderers.put(
                "projects.project_closed",
                (env, secrets) -> success("Project closed", "The project has been closed."));
        renderers.put(
                "finance.invoice_issued",
                (env, secrets) -> {
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
                (env, secrets) -> success("Payment received", "We have received your payment. Thank you."));
        renderers.put(
                "finance.invoice_voided",
                (env, secrets) -> warning("Invoice voided", "An invoice was voided."));
        renderers.put(
                "commitments.ticket_opened",
                (env, secrets) -> info("Ticket opened", "A new ticket has been opened."));
        renderers.put(
                "commitments.ticket_replied",
                (env, secrets) -> info("Ticket reply", "There is a new reply on your ticket."));
        renderers.put(
                "commitments.ticket_status_changed",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Ticket status changed",
                            "Status: " + p.path("to_status").asText());
                });
        // C-1 / C-2: careers module notifications + inquiry acknowledgment.
        renderers.put(
                "careers.posting_published",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    return info(
                            "Job posting published",
                            "\"" + p.path("title").asText() + "\" is now live on the careers page.");
                });
        renderers.put(
                "careers.application_submitted",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    String name = p.path("name").asText("");
                    String posting = p.path("posting_title").asText("");
                    return info(
                            "Thanks for applying to EDSS",
                            "Hi "
                                    + (name.isBlank() ? "there" : name)
                                    + ",\n\nWe received your application for "
                                    + posting
                                    + ". Our team reviews every application personally; we "
                                    + "will get back to you within the next two weeks.\n\n"
                                    + "— The EDSS team");
                });
        renderers.put(
                "careers.application_reviewed",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    String name = p.path("name").asText("");
                    String posting = p.path("posting_title").asText("");
                    String status = p.path("status").asText("");
                    return switch (status) {
                        case "contacted" -> info(
                                "We would like to talk",
                                "Hi "
                                        + name
                                        + ",\n\nThank you for applying for "
                                        + posting
                                        + ". Our team would like to move forward — please expect a "
                                        + "personal note in the next few days.");
                        case "hired" -> success(
                                "Welcome to EDSS",
                                "Hi "
                                        + name
                                        + ",\n\nWe are delighted to bring you on board for "
                                        + posting
                                        + ". Onboarding details will follow shortly.");
                        case "rejected" -> info(
                                "Application update",
                                "Hi "
                                        + name
                                        + ",\n\nThank you again for applying for "
                                        + posting
                                        + ". We are not able to move forward at this time, but we "
                                        + "appreciate the effort you put into your application.");
                        default -> info(
                                "Application update",
                                "Your application for " + posting + " is now marked as: " + status + ".");
                    };
                });
        renderers.put(
                "relationship.inquiry_acknowledged",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    String name = p.path("name").asText("");
                    return info(
                            "Thanks for reaching out",
                            "Hi "
                                    + (name.isBlank() ? "there" : name)
                                    + ",\n\nWe received your inquiry and it is now with our team. "
                                    + "Someone will respond within one business day.\n\n"
                                    + "— The EDSS team");
                });
        renderers.put(
                "identity.password_reset_requested",
                (env, secrets) -> {
                    JsonNode p = (JsonNode) env.payload();
                    String handle = p.path("reset_token_handle").asText();
                    Optional<String> token = secrets.pop(handle);
                    if (token.isEmpty()) {
                        return info(
                                "Password reset requested",
                                "Please request another reset link — the previous one"
                                        + " could not be delivered.");
                    }
                    return info(
                            "Reset your EDSS password",
                            "Use this token to reset your password:\n\n"
                                    + token.get()
                                    + "\n\nExpires in 30 minutes.");
                });
        return Map.copyOf(renderers);
    }

    public NotificationCopy resolve(EventEnvelope envelope) {
        Renderer renderer = RENDERERS.get(envelope.eventType());
        if (renderer == null) {
            return info("Notification", envelope.eventType());
        }
        return renderer.render(envelope, ephemeralSecrets);
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
