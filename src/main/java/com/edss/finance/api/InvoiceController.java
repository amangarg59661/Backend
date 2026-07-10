package com.edss.finance.api;

import com.edss.finance.api.dto.CreateInvoiceRequest;
import com.edss.finance.api.dto.InvoiceDto;
import com.edss.finance.application.InvoiceService;
import com.edss.finance.domain.Invoice;
import com.edss.shared.api.PaginatedResponse;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "invoices", description = "Client billing with Stripe / Razorpay / manual providers.")
public class InvoiceController {

    private final InvoiceService invoices;

    public InvoiceController(InvoiceService invoices) {
        this.invoices = invoices;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<InvoiceDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        boolean isStaff = isStaff(principal);
        List<InvoiceDto> items =
                invoices.list(principal.userId(), isStaff, limit).stream()
                        .map(InvoiceController::toDto)
                        .toList();
        return new PaginatedResponse<>(items, null, false);
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public InvoiceDto fetch(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID invoiceId) {
        Invoice invoice = invoices.fetch(invoiceId);
        enforceReadAccess(principal, invoice);
        return toDto(invoice);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:invoice:create') or hasAuthority('finance:*') or hasAuthority('admin:*')")
    public InvoiceDto create(@Valid @RequestBody CreateInvoiceRequest req) {
        List<InvoiceService.LineItem> items =
                req.lineItems().stream()
                        .map(li -> new InvoiceService.LineItem(li.description(), li.amountMinor()))
                        .toList();
        InvoiceService.NewInvoice spec =
                new InvoiceService.NewInvoice(
                        req.clientUserId(),
                        req.clientEmail(),
                        req.projectId(),
                        req.milestoneId(),
                        req.amountMinor(),
                        req.currency(),
                        req.provider(),
                        req.description(),
                        req.dueAt(),
                        items);
        return toDto(invoices.issue(spec, req.successUrl(), req.cancelUrl()));
    }

    @PostMapping("/{invoiceId}/mark-paid")
    @PreAuthorize("hasAuthority('finance:invoice:mark-paid') or hasAuthority('finance:*') or hasAuthority('admin:*')")
    public InvoiceDto markPaid(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID invoiceId) {
        return toDto(invoices.markPaidManually(invoiceId, principal.userId()));
    }

    @PostMapping("/{invoiceId}/void")
    @PreAuthorize("hasAuthority('finance:invoice:void') or hasAuthority('finance:*') or hasAuthority('admin:*')")
    public InvoiceDto voidInvoice(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID invoiceId) {
        return toDto(invoices.void_(invoiceId, principal.userId()));
    }

    private static boolean isStaff(AuthenticatedUser principal) {
        return "staff".equals(principal.primaryRole()) || principal.hasBothRoles();
    }

    private static void enforceReadAccess(AuthenticatedUser principal, Invoice invoice) {
        if (isStaff(principal)) {
            return;
        }
        if (!invoice.getClientUserId().equals(principal.userId())) {
            throw new com.edss.shared.api.ApiException(
                    com.edss.shared.api.ApiErrorCode.FORBIDDEN, "Not your invoice.");
        }
    }

    private static InvoiceDto toDto(Invoice i) {
        return new InvoiceDto(
                i.getId(),
                i.getClientUserId(),
                i.getProjectId(),
                i.getMilestoneId(),
                i.getNumber(),
                i.getAmountMinor(),
                i.getCurrency(),
                i.getStatus(),
                i.getProvider(),
                i.getPaymentLink(),
                i.getIssuedAt(),
                i.getDueAt(),
                i.getPaidAt(),
                i.getCreatedAt());
    }
}
