package com.edss.projects.infrastructure;

import com.edss.projects.domain.OnboardingCall;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingCallRepository extends JpaRepository<OnboardingCall, UUID> {

    Optional<OnboardingCall> findByProjectId(UUID projectId);
}
