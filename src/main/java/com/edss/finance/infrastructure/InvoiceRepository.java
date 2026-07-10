package com.edss.finance.infrastructure;

import com.edss.finance.domain.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId, Limit limit);

    List<Invoice> findAllByOrderByCreatedAtDesc(Limit limit);

    Optional<Invoice> findByProviderPaymentIntentId(String providerPaymentIntentId);
}
