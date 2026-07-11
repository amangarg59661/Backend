package com.edss.notifications.api;

import com.edss.notifications.api.dto.NotificationDto;
import com.edss.notifications.domain.Notification;
import com.edss.notifications.infrastructure.NotificationRepository;
import com.edss.shared.api.PaginatedResponse;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "notifications", description = "User notification inbox.")
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public PaginatedResponse<NotificationDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "50") int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        List<Notification> rows =
                unread
                        ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(
                                principal.userId(), lim)
                        : notifications.findByUserIdOrderByCreatedAtDesc(principal.userId(), lim);
        return new PaginatedResponse<>(rows.stream().map(NotificationController::toDto).toList(), null, false);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Map.of("count", notifications.countByUserIdAndReadFalse(principal.userId()));
    }

    @PostMapping("/{notificationId}/read")
    @Transactional
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID notificationId) {
        notifications.findById(notificationId)
                .filter(n -> n.getUserId().equals(principal.userId()))
                .ifPresent(Notification::markRead);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Transactional
    public Map<String, Integer> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        int touched = notifications.markAllRead(principal.userId());
        return Map.of("updated", touched);
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getUserId(),
                n.getSeverity(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.getCreatedAt(),
                n.getHref(),
                n.getEventType());
    }
}
