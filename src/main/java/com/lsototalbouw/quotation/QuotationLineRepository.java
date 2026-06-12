package com.lsototalbouw.quotation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationLineRepository extends JpaRepository<QuotationLine, Long> {

    List<QuotationLine> findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(Long companyId, Long quotationId);

    Optional<QuotationLine> findByQuotationCompanyAccountIdAndQuotationIdAndIdAndActiveTrue(Long companyId, Long quotationId, Long id);

    @Query("select coalesce(sum((l.quantity * l.unitPrice) + ((l.quantity * l.unitPrice) * l.vatRate / 100)), 0) "
            + "from QuotationLine l where l.quotation.id = :quotationId and l.active = true")
    BigDecimal totalByQuotationId(@Param("quotationId") Long quotationId);

    long countByQuotationIdAndActiveTrue(Long quotationId);
}
