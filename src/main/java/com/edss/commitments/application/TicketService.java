package com.edss.commitments.application;

import com.edss.commitments.domain.Ticket;
import com.edss.commitments.domain.TicketMessage;
import com.edss.commitments.domain.TicketStatus;
import com.edss.commitments.domain.events.CommitmentsEvents;
import com.edss.commitments.infrastructure.TicketMessageRepository;
import com.edss.commitments.infrastructure.TicketRepository;
import com.edss.projects.spi.ProjectsQuery;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository tickets;
    private final TicketMessageRepository messages;
    private final ProjectsQuery projectsQuery;
    private final OutboxWriter outbox;
    private final Clock clock;

    public TicketService(
            TicketRepository tickets,
            TicketMessageRepository messages,
            ProjectsQuery projectsQuery,
            OutboxWriter outbox,
            Clock clock) {
        this.tickets = tickets;
        this.messages = messages;
        this.projectsQuery = projectsQuery;
        this.outbox = outbox;
        this.clock = clock;
    }

    public Ticket open(NewTicket spec, UUID actorUserId) {
        Instant now = clock.instant();
        if (spec.projectId() != null) {
            Optional<ProjectsQuery.ProjectSummary> project = projectsQuery.findProject(spec.projectId());
            if (project.isEmpty()) {
                throw new ApiException(ApiErrorCode.NOT_FOUND, "Project not found.");
            }
            if (spec.isMaintenance() && !project.get().isInMaintenanceWindow(now)) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_FAILED,
                        "Maintenance tickets require the project to be in the maintenance window.");
            }
        } else if (spec.isMaintenance()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Maintenance tickets must reference a project.");
        }

        Ticket ticket =
                new Ticket(
                        UUID.randomUUID(),
                        actorUserId,
                        spec.projectId(),
                        spec.subject(),
                        spec.description(),
                        spec.priority(),
                        spec.isMaintenance(),
                        now);
        tickets.save(ticket);
        outbox.append(
                "commitments",
                new CommitmentsEvents.TicketOpened(
                        UUID.randomUUID(),
                        now,
                        ticket.getId(),
                        actorUserId,
                        spec.projectId(),
                        ticket.getPriority(),
                        spec.isMaintenance()),
                Map.of(
                        "ticket_id", ticket.getId(),
                        "raised_by_user_id", actorUserId,
                        "project_id",
                        spec.projectId() == null ? "" : spec.projectId().toString(),
                        "priority", ticket.getPriority(),
                        "is_maintenance", spec.isMaintenance()));
        log.info("Ticket {} opened by {}", ticket.getId(), actorUserId);
        return ticket;
    }

    public TicketMessage reply(UUID ticketId, UUID authorUserId, String body) {
        Ticket ticket = fetch(ticketId);
        Instant now = clock.instant();
        ticket.touch(now);
        TicketMessage message =
                new TicketMessage(UUID.randomUUID(), ticketId, authorUserId, body, now);
        messages.save(message);
        outbox.append(
                "commitments",
                new CommitmentsEvents.TicketReplied(
                        UUID.randomUUID(), now, ticketId, message.getId(), authorUserId),
                Map.of(
                        "ticket_id", ticketId,
                        "message_id", message.getId(),
                        "author_user_id", authorUserId));
        return message;
    }

    public Ticket changeStatus(UUID ticketId, TicketStatus target, UUID actorUserId) {
        Ticket ticket = fetch(ticketId);
        TicketStatus from = ticket.getStatus();
        if (from == target) {
            return ticket;
        }
        Instant now = clock.instant();
        ticket.changeStatus(target, now);
        outbox.append(
                "commitments",
                new CommitmentsEvents.TicketStatusChanged(
                        UUID.randomUUID(), now, ticketId, from.wire(), target.wire(), actorUserId),
                Map.of(
                        "ticket_id", ticketId,
                        "from_status", from.wire(),
                        "to_status", target.wire(),
                        "actor_user_id", actorUserId));
        return ticket;
    }

    public Ticket assign(UUID ticketId, UUID assigneeUserId, UUID actorUserId) {
        Ticket ticket = fetch(ticketId);
        Instant now = clock.instant();
        ticket.assign(assigneeUserId, now);
        return ticket;
    }

    @Transactional(readOnly = true)
    public Ticket fetch(UUID ticketId) {
        return tickets.findById(ticketId)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Ticket not found."));
    }

    @Transactional(readOnly = true)
    public List<Ticket> list(
            UUID actorUserId, boolean isStaff, UUID projectId, UUID assigneeUserId, int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        if (assigneeUserId != null) {
            return tickets.findByAssigneeUserIdOrderByUpdatedAtDesc(assigneeUserId, lim);
        }
        if (projectId != null) {
            return tickets.findByProjectIdOrderByUpdatedAtDesc(projectId, lim);
        }
        return isStaff
                ? tickets.findAllByOrderByUpdatedAtDesc(lim)
                : tickets.findByRaisedByUserIdOrderByUpdatedAtDesc(actorUserId, lim);
    }

    @Transactional(readOnly = true)
    public List<TicketMessage> messagesFor(UUID ticketId) {
        return messages.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public record NewTicket(
            UUID projectId,
            String subject,
            String description,
            String priority,
            boolean isMaintenance) {}
}
