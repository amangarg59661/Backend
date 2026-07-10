package com.edss.identity.infrastructure;

import com.edss.identity.domain.TrustedDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {

    Optional<TrustedDevice> findByDeviceTokenHash(String deviceTokenHash);

    List<TrustedDevice> findByUserIdAndRevokedAtIsNull(UUID userId);
}
