package com.edss.finance.infrastructure;

import com.edss.finance.domain.Invoice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {}
