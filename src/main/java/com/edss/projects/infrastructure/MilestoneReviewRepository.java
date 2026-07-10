package com.edss.projects.infrastructure;

import com.edss.projects.domain.MilestoneReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneReviewRepository extends JpaRepository<MilestoneReview, UUID> {

    List<MilestoneReview> findByMilestoneIdOrderByReviewedAtDesc(UUID milestoneId);
}
