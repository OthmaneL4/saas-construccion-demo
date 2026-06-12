package com.lsototalbouw.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(Long companyId, Long invoiceId);

    Optional<InvoiceLine> findByInvoiceCompanyAccountIdAndInvoiceIdAndIdAndActiveTrue(Long companyId, Long invoiceId, Long id);

    @Query("select coalesce(sum((l.quantity * l.unitPrice) + ((l.quantity * l.unitPrice) * l.vatRate / 100)), 0) "
            + "from InvoiceLine l where l.invoice.id = :invoiceId and l.active = true")
    BigDecimal totalByInvoiceId(@Param("invoiceId") Long invoiceId);

    long countByInvoiceIdAndActiveTrue(Long invoiceId);
}
