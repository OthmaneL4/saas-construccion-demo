package com.lsototalbouw.invoice;

import com.lsototalbouw.common.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Query("select i from Invoice i join fetch i.customer left join fetch i.project where i.active = true order by i.dueDate asc")
    List<Invoice> findByActiveTrueOrderByDueDateAsc();

    @Query("select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.active = true order by i.dueDate asc")
    List<Invoice> findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(@Param("companyId") Long companyId);

    @Query("select i from Invoice i join fetch i.customer left join fetch i.project "
            + "where i.companyAccount.id = :companyId and i.active = true and i.status in :statuses "
            + "order by i.dueDate asc")
    List<Invoice> findOpenByCompanyId(@Param("companyId") Long companyId,
                                      @Param("statuses") Collection<InvoiceStatus> statuses);

    @Query("select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.customer.id = :customerId and i.active = true order by i.dueDate asc")
    List<Invoice> findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByDueDateAsc(@Param("companyId") Long companyId,
                                                                                    @Param("customerId") Long customerId);

    @Query("select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.project.id = :projectId and i.active = true order by i.dueDate asc")
    List<Invoice> findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByDueDateAsc(@Param("companyId") Long companyId,
                                                                                   @Param("projectId") Long projectId);

    @Query(value = "select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.active = true",
            countQuery = "select count(i) from Invoice i where i.companyAccount.id = :companyId and i.active = true")
    Page<Invoice> findPageByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.active = true "
            + "and (:status is null or i.status = :status) "
            + "and (:dueFrom is null or i.dueDate >= :dueFrom) "
            + "and (:dueTo is null or i.dueDate <= :dueTo) "
            + "and (:query is null or lower(i.invoiceNumber) like lower(concat('%', :query, '%')) "
            + "or lower(i.customer.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(i.project.name, '')) like lower(concat('%', :query, '%'))) "
            + "order by i.dueDate asc")
    List<Invoice> searchByCompanyId(@Param("companyId") Long companyId, @Param("query") String query,
                                    @Param("status") InvoiceStatus status,
                                    @Param("dueFrom") LocalDate dueFrom,
                                    @Param("dueTo") LocalDate dueTo);

    @Query(value = "select i from Invoice i join fetch i.customer left join fetch i.project where i.companyAccount.id = :companyId and i.active = true "
            + "and (:status is null or i.status = :status) "
            + "and (:dueFrom is null or i.dueDate >= :dueFrom) "
            + "and (:dueTo is null or i.dueDate <= :dueTo) "
            + "and (:query is null or lower(i.invoiceNumber) like lower(concat('%', :query, '%')) "
            + "or lower(i.customer.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(i.project.name, '')) like lower(concat('%', :query, '%')))",
            countQuery = "select count(i) from Invoice i join i.customer left join i.project where i.companyAccount.id = :companyId and i.active = true "
                    + "and (:status is null or i.status = :status) "
                    + "and (:dueFrom is null or i.dueDate >= :dueFrom) "
                    + "and (:dueTo is null or i.dueDate <= :dueTo) "
                    + "and (:query is null or lower(i.invoiceNumber) like lower(concat('%', :query, '%')) "
                    + "or lower(i.customer.name) like lower(concat('%', :query, '%')) "
                    + "or lower(coalesce(i.project.name, '')) like lower(concat('%', :query, '%')))")
    Page<Invoice> searchPageByCompanyId(@Param("companyId") Long companyId, @Param("query") String query,
                                        @Param("status") InvoiceStatus status,
                                        @Param("dueFrom") LocalDate dueFrom,
                                        @Param("dueTo") LocalDate dueTo,
                                        Pageable pageable);

    long countByStatusInAndActiveTrue(Collection<InvoiceStatus> statuses);

    long countByCompanyAccountIdAndStatusInAndActiveTrue(Long companyId, Collection<InvoiceStatus> statuses);

    @Query("select coalesce(sum(i.amount), 0) from Invoice i where i.active = true")
    BigDecimal totalInvoiced();

    @Query("select coalesce(sum(i.amount), 0) from Invoice i where i.companyAccount.id = :companyId and i.active = true")
    BigDecimal totalInvoicedByCompanyId(@Param("companyId") Long companyId);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.active = true")
    BigDecimal totalPaid();

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.companyAccount.id = :companyId and i.active = true")
    BigDecimal totalPaidByCompanyId(@Param("companyId") Long companyId);

    @Query("select coalesce(sum(i.amount), 0) from Invoice i where i.companyAccount.id = :companyId and i.project.id = :projectId and i.active = true")
    BigDecimal totalInvoicedByCompanyIdAndProjectId(@Param("companyId") Long companyId, @Param("projectId") Long projectId);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.companyAccount.id = :companyId and i.project.id = :projectId and i.active = true")
    BigDecimal totalPaidByCompanyIdAndProjectId(@Param("companyId") Long companyId, @Param("projectId") Long projectId);

    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);

    boolean existsByCompanyAccountIdAndInvoiceNumberIgnoreCase(Long companyId, String invoiceNumber);

    boolean existsByCompanyAccountIdAndInvoiceNumberIgnoreCaseAndIdNot(Long companyId, String invoiceNumber, Long id);

    @Query("select i.invoiceNumber from Invoice i where i.companyAccount.id = :companyId")
    List<String> findInvoiceNumbersByCompanyId(@Param("companyId") Long companyId);

    Optional<Invoice> findByCompanyAccountIdAndQuotationIdAndActiveTrue(Long companyId, Long quotationId);

    boolean existsByCompanyAccountIdAndQuotationIdAndActiveTrue(Long companyId, Long quotationId);

    @EntityGraph(attributePaths = {"companyAccount", "customer", "project", "quotation"})
    Optional<Invoice> findByCompanyAccountIdAndInvoiceNumberIgnoreCase(Long companyId, String invoiceNumber);

    @EntityGraph(attributePaths = {"companyAccount", "customer", "project", "quotation"})
    Optional<Invoice> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
