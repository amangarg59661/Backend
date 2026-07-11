package com.edss.notifications.infrastructure;

import com.edss.notifications.domain.WhatsappDelivery;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappDeliveryRepository extends JpaRepository<WhatsappDelivery, UUID> {}
