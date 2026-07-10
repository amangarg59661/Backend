package com.edss.relationship.api;

import com.edss.relationship.api.dto.InquiryDto;
import com.edss.relationship.api.dto.InquiryStatusUpdateRequest;
import com.edss.relationship.application.InquiryService;
import com.edss.relationship.domain.Inquiry;
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

/**
 * Staff triage endpoints. All require the {@code relationship:inquiry:*}
 * permission family. Convert is a distinct action so its ownership
 * consequences (creating a real user) are auditable in the frontend
 * separately from mundane status flips.
 */
@RestController
@RequestMapping("/api/v1/staff/inquiries")
@Tag(name = "inquiries-staff", description = "Staff triage of inbound inquiries.")
public class StaffInquiryController {

    private final InquiryService inquiries;

    public StaffInquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('relationship:inquiry:read') or hasAuthority('relationship:inquiry:*') or hasAuthority('admin:*')")
    public List<InquiryDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return inquiries.list(status, limit).stream().map(StaffInquiryController::toDto).toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('relationship:inquiry:write') or hasAuthority('relationship:inquiry:*') or hasAuthority('admin:*')")
    public InquiryDto patch(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody InquiryStatusUpdateRequest req) {
        Inquiry row =
                switch (req.status()) {
                    case "in_review" -> inquiries.moveToInReview(id, principal.userId());
                    case "rejected" -> inquiries.reject(id, principal.userId());
                    default -> throw new IllegalStateException("Unreachable status: " + req.status());
                };
        return toDto(row);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('relationship:inquiry:convert') or hasAuthority('relationship:inquiry:*') or hasAuthority('admin:*')")
    public InquiryDto convert(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return toDto(inquiries.convert(id, principal.userId()));
    }

    private static InquiryDto toDto(Inquiry row) {
        return new InquiryDto(
                row.getId(),
                row.getName(),
                row.getEmail(),
                row.getPhone(),
                row.getService(),
                row.getMessage(),
                row.getStatus().wire(),
                row.getSource(),
                row.getConvertedToUserId(),
                row.getSubmittedAt(),
                row.getReviewedAt(),
                row.getReviewedByUserId());
    }
}
