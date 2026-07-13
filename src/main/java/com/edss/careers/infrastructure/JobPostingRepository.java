package com.edss.careers.infrastructure;

import com.edss.careers.domain.JobPosting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    Optional<JobPosting> findBySlug(String slug);

    List<JobPosting> findByStatusOrderByPublishedAtDesc(String status, Limit limit);

    List<JobPosting> findAllByOrderByUpdatedAtDesc(Limit limit);
}
