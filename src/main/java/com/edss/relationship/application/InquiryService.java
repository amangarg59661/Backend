package com.edss.relationship.application;

import com.edss.identity.spi.IdentityUserProvisioning;
import com.edss.relationship.domain.Inquiry;
import com.edss.relationship.domain.InquiryStatus;
import com.edss.relationship.domain.events.RelationshipEvents;
import com.edss.relationship.infrastructure.InquiryRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.ratelimit.RateLimitDecision;
import com.edss.shared.ratelimit.RateLimiter;
import com.edss.shared.security.EphemeralSecrets;
import java.time.Duration;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final Duration INQUIRY_WINDOW = Duration.ofHours(1);
    private static final int INQUIRY_LIMIT_PER_IP = 5;

    private static final Duration INVITE_HANDLE_TTL = Duration.ofDays(7);

    private final InquiryRepository inquiries;
    private final IdentityUserProvisioning identityProvisioning;
    private final OutboxWriter outbox;
    private final RateLimiter rateLimiter;
    private final EphemeralSecrets ephemeralSecrets;
    private final Clock clock;

    public InquiryService(
            InquiryRepository inquiries,
            IdentityUserProvisioning identityProvisioning,
            OutboxWriter outbox,
            RateLimiter rateLimiter,
            EphemeralSecrets ephemeralSecrets,
            Clock clock) {
        this.inquiries = inquiries;
        this.identityProvisioning = identityProvisioning;
        this.outbox = outbox;
        this.rateLimiter = rateLimiter;
        this.ephemeralSecrets = ephemeralSecrets;
        this.clock = clock;
    }

    public Inquiry submit(
            String name,
            String email,
            String phone,
            String service,
            String message,
            String source,
            String ipAddress) {
        enforceRateLimit(ipAddress);
        Instant now = clock.instant();
        Inquiry row =
                new Inquiry(
                        UUID.randomUUID(), name, email, phone, service, message, source, now);
        inquiries.save(row);
        outbox.append(
                "relationship",
                new RelationshipEvents.InquirySubmitted(
                        UUID.randomUUID(), now, row.getId(), email, service),
                Map.of(
                        "inquiry_id", row.getId(),
                        "email", email,
                        "service", service == null ? "" : service));
        // C-2: separate acknowledgment event so the notifications router
        // can send a "thanks for reaching out" auto email to the submitter
        // without also targeting the staff triage recipients.
        outbox.append(
                "relationship",
                new RelationshipEvents.InquiryAcknowledged(
                        UUID.randomUUID(), now, row.getId(), email, name == null ? "" : name),
                Map.of(
                        "inquiry_id", row.getId(),
                        "email", email,
                        "name", name == null ? "" : name));
        return row;
    }

    @Transactional(readOnly = true)
    public List<Inquiry> list(String status, int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        if (status == null || status.isBlank()) {
            return inquiries.findAllByOrderBySubmittedAtDesc(lim);
        }
        InquiryStatus parsed;
        try {
            parsed = InquiryStatus.ofWire(status);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Unknown status.");
        }
        return inquiries.findByStatusOrderBySubmittedAtDesc(parsed.wire(), lim);
    }

    public Inquiry moveToInReview(UUID inquiryId, UUID reviewerId) {
        Inquiry row = fetch(inquiryId);
        row.moveToInReview(reviewerId, clock.instant());
        return row;
    }

    public Inquiry reject(UUID inquiryId, UUID reviewerId) {
        Inquiry row = fetch(inquiryId);
        row.reject(reviewerId, clock.instant());
        return row;
    }

    public Inquiry convert(UUID inquiryId, UUID reviewerId) {
        Inquiry row = fetch(inquiryId);
        if (row.getStatus() == InquiryStatus.CONVERTED) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Inquiry already converted.");
        }
        IdentityUserProvisioning.InviteResult invite;
        try {
            invite = identityProvisioning.createInvited(row.getEmail(), row.getName(), "client");
        } catch (IdentityUserProvisioning.EmailAlreadyExistsException ex) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, ex.getMessage());
        }
        Instant now = clock.instant();
        row.markConverted(reviewerId, invite.userId(), now);
        // Stash the invite token behind a short-TTL handle. The outbox row is
        // opaque; the notifier pops the handle to obtain the plaintext exactly
        // once when it sends the invite email.
        String inviteHandle = ephemeralSecrets.stash(invite.inviteToken(), INVITE_HANDLE_TTL);
        outbox.append(
                "relationship",
                new RelationshipEvents.InquiryConverted(
                        UUID.randomUUID(),
                        now,
                        row.getId(),
                        invite.userId(),
                        row.getEmail(),
                        row.getName(),
                        inviteHandle),
                Map.of(
                        "inquiry_id", row.getId(),
                        "user_id", invite.userId(),
                        "email", row.getEmail(),
                        "name", row.getName(),
                        "invite_token_handle", inviteHandle));
        log.info("Converted inquiry {} → user {}", row.getId(), invite.userId());
        return row;
    }

    private Inquiry fetch(UUID id) {
        return inquiries.findById(id)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Inquiry not found."));
    }

    private void enforceRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        RateLimitDecision decision =
                rateLimiter.hit("inquiry:ip:" + ipAddress, INQUIRY_LIMIT_PER_IP, INQUIRY_WINDOW);
        if (!decision.allowed()) {
            throw new ApiException(
                    ApiErrorCode.RATE_LIMITED,
                    "Too many inquiries. Try again later.",
                    Map.of("retry_after", decision.retryAfter().getSeconds()));
        }
    }
}
