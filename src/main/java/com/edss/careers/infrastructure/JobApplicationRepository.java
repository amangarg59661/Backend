package com.edss.careers.infrastructure;

import com.edss.careers.domain.JobApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByJobPostingIdOrderBySubmittedAtDesc(UUID jobPostingId, Limit limit);

    long countByJobPostingIdAndApplicantEmail(UUID jobPostingId, String applicantEmail);
}
