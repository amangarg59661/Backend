package com.edss.integrations.calendar.infrastructure;

import com.edss.integrations.calendar.domain.GoogleCalendarToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, UUID> {}
