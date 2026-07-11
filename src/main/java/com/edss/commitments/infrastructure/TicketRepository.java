package com.edss.commitments.infrastructure;

import com.edss.commitments.domain.Ticket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByRaisedByUserIdOrderByUpdatedAtDesc(UUID raisedByUserId, Limit limit);

    List<Ticket> findByProjectIdOrderByUpdatedAtDesc(UUID projectId, Limit limit);

    List<Ticket> findByAssigneeUserIdOrderByUpdatedAtDesc(UUID assigneeUserId, Limit limit);

    List<Ticket> findAllByOrderByUpdatedAtDesc(Limit limit);
}
