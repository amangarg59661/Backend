package com.edss.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateInvoiceRequest(
        @NotNull UUID clientUserId,
        @Email @NotBlank String clientEmail,
        UUID projectId,
        UUID milestoneId,
        @NotBlank @Pattern(regexp = "^(stripe|razorpay|manual)$") String provider,
        @Min(0) long amountMinor,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Size(max = 500) String description,
        Instant dueAt,
        @Valid @NotEmpty List<LineItem> lineItems,
        @Size(max = 500) String successUrl,
        @Size(max = 500) String cancelUrl) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineItem(@NotBlank @Size(max = 200) String description, @Min(0) long amountMinor) {}
}
