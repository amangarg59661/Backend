package com.edss.relationship.infrastructure;

import com.edss.relationship.domain.Inquiry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    List<Inquiry> findByStatusOrderBySubmittedAtDesc(String status, Limit limit);

    List<Inquiry> findAllByOrderBySubmittedAtDesc(Limit limit);
}
