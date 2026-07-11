package com.edss.relationship.api;

import com.edss.relationship.api.dto.InquiryDto;
import com.edss.relationship.api.dto.InquirySubmitRequest;
import com.edss.relationship.application.InquiryService;
import com.edss.relationship.domain.Inquiry;
import com.edss.shared.api.HttpRequests;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anonymous inquiry submission. Rate-limited to 5/hour per source IP.
 * Response omits internal fields (converted_to_user_id, reviewed_*).
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@Tag(name = "inquiries-public", description = "Public inquiry submission form.")
public class PublicInquiryController {

    private final InquiryService inquiries;

    public PublicInquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InquiryDto submit(
            @Valid @RequestBody InquirySubmitRequest req, HttpServletRequest http) {
        Inquiry row =
                inquiries.submit(
                        req.name(),
                        req.email(),
                        req.phone(),
                        req.service(),
                        req.message(),
                        req.source(),
                        clientIp(http));
        return new InquiryDto(
                row.getId(),
                row.getName(),
                row.getEmail(),
                null,
                row.getService(),
                null,
                row.getStatus().wire(),
                null,
                null,
                row.getSubmittedAt(),
                null,
                null);
    }

    private static String clientIp(HttpServletRequest request) {
        return HttpRequests.clientIp(request);
    }
}
