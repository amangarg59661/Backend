package com.edss.commitments.infrastructure;

import com.edss.commitments.domain.Ticket;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {}
