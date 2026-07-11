package com.edss.commitments.api;

import com.edss.commitments.api.dto.OpenTicketRequest;
import com.edss.commitments.api.dto.TicketDto;
import com.edss.commitments.api.dto.TicketMessageDto;
import com.edss.commitments.api.dto.TicketMessageRequest;
import com.edss.commitments.api.dto.TicketPatchRequest;
import com.edss.commitments.application.TicketService;
import com.edss.commitments.domain.Ticket;
import com.edss.commitments.domain.TicketMessage;
import com.edss.commitments.domain.TicketStatus;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.api.PaginatedResponse;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@PreAuthorize("isAuthenticated()")
@Tag(name = "tickets", description = "Support and maintenance tickets with reply threads.")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public PaginatedResponse<TicketDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID assigneeUserId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        boolean isStaff = isStaff(principal);
        List<TicketDto> items =
                tickets.list(principal.userId(), isStaff, projectId, assigneeUserId, limit).stream()
                        .map(TicketController::toDto)
                        .toList();
        return new PaginatedResponse<>(items, null, false);
    }

    @GetMapping("/{ticketId}")
    public TicketDto fetch(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID ticketId) {
        Ticket ticket = tickets.fetch(ticketId);
        enforceRead(principal, ticket);
        return toDto(ticket);
    }

    @PostMapping
    public TicketDto open(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody OpenTicketRequest req) {
        TicketService.NewTicket spec =
                new TicketService.NewTicket(
                        req.projectId(),
                        req.subject(),
                        req.description(),
                        req.priority(),
                        req.isMaintenance());
        return toDto(tickets.open(spec, principal.userId()));
    }

    @GetMapping("/{ticketId}/messages")
    public List<TicketMessageDto> messages(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID ticketId) {
        enforceRead(principal, tickets.fetch(ticketId));
        return tickets.messagesFor(ticketId).stream().map(TicketController::toDto).toList();
    }

    @PostMapping("/{ticketId}/messages")
    public TicketMessageDto reply(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID ticketId,
            @Valid @RequestBody TicketMessageRequest req) {
        Ticket ticket = tickets.fetch(ticketId);
        enforceRead(principal, ticket);
        return toDto(tickets.reply(ticketId, principal.userId(), req.body()));
    }

    @PatchMapping("/{ticketId}")
    @PreAuthorize("hasAuthority('commitments:ticket:write') or hasAuthority('commitments:ticket:*') or hasAuthority('admin:*')")
    public TicketDto patch(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID ticketId,
            @Valid @RequestBody TicketPatchRequest req) {
        Ticket ticket = tickets.fetch(ticketId);
        if (req.status() != null) {
            ticket = tickets.changeStatus(ticketId, TicketStatus.ofWire(req.status()), principal.userId());
        }
        if (req.assigneeUserId() != null) {
            ticket = tickets.assign(ticketId, req.assigneeUserId(), principal.userId());
        }
        return toDto(ticket);
    }

    private static boolean isStaff(AuthenticatedUser principal) {
        return "staff".equals(principal.primaryRole()) || principal.hasBothRoles();
    }

    private static void enforceRead(AuthenticatedUser principal, Ticket ticket) {
        if (isStaff(principal)) {
            return;
        }
        if (!ticket.getRaisedByUserId().equals(principal.userId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not your ticket.");
        }
    }

    private static TicketDto toDto(Ticket t) {
        return new TicketDto(
                t.getId(),
                t.getRaisedByUserId(),
                t.getProjectId(),
                t.getSubject(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus().wire(),
                t.getAssigneeUserId(),
                t.isMaintenance(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    private static TicketMessageDto toDto(TicketMessage m) {
        return new TicketMessageDto(
                m.getId(), m.getTicketId(), m.getAuthorUserId(), m.getBody(), m.getCreatedAt());
    }
}
