package com.edss.finance.api;

import com.edss.finance.api.dto.InvoiceDto;
import com.edss.shared.api.PaginatedResponse;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "invoices", description = "Client billing.")
public class InvoiceController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<InvoiceDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return PaginatedResponse.empty();
    }
}
