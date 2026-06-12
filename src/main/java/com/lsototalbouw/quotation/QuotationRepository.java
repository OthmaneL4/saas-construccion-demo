package com.lsototalbouw.quotation;

import com.lsototalbouw.common.enums.QuotationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    @EntityGraph(attributePaths = "customer")
    List<Quotation> findByCompanyAccountIdAndActiveTrueOrderByIssueDateDesc(Long companyId);

    @EntityGraph(attributePaths = "customer")
    List<Quotation> findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByIssueDateDesc(Long companyId,
                                                                                         Long customerId);

    @EntityGraph(attributePaths = "customer")
    @Query("""
            select quotation
            from Quotation quotation
            join quotation.customer customer
            where quotation.companyAccount.id = :companyId
              and quotation.active = true
              and (:customerId is null or customer.id = :customerId)
              and (:status is null or quotation.status = :status)
              and (:validFrom is null or quotation.expiryDate >= :validFrom)
              and (:validTo is null or quotation.expiryDate <= :validTo)
              and (
                    :query is null
                 or lower(quotation.quotationNumber) like lower(concat('%', :query, '%'))
                 or lower(quotation.title) like lower(concat('%', :query, '%'))
                 or lower(customer.name) like lower(concat('%', :query, '%'))
              )
            order by quotation.issueDate desc
            """)
    List<Quotation> searchByCompanyId(@Param("companyId") Long companyId,
                                      @Param("customerId") Long customerId,
                                      @Param("status") QuotationStatus status,
                                      @Param("query") String query,
                                      @Param("validFrom") LocalDate validFrom,
                                      @Param("validTo") LocalDate validTo);

    @EntityGraph(attributePaths = {"companyAccount", "customer"})
    Optional<Quotation> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);

    boolean existsByCompanyAccountIdAndQuotationNumberIgnoreCase(Long companyId, String quotationNumber);

    boolean existsByCompanyAccountIdAndQuotationNumberIgnoreCaseAndIdNot(Long companyId, String quotationNumber, Long id);

    @EntityGraph(attributePaths = {"companyAccount", "customer"})
    Optional<Quotation> findByCompanyAccountIdAndQuotationNumberIgnoreCase(Long companyId, String quotationNumber);

    @Query("select q.quotationNumber from Quotation q where q.companyAccount.id = :companyId")
    List<String> findQuotationNumbersByCompanyId(@Param("companyId") Long companyId);
}
