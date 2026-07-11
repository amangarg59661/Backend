package com.edss.integrations.calendar.infrastructure;

import com.edss.integrations.calendar.domain.CalendarWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarWebhookEventRepository
        extends JpaRepository<CalendarWebhookEvent, UUID> {

    Optional<CalendarWebhookEvent> findByProviderAndExternalEventId(
            String provider, String externalEventId);
}
