package com.lsototalbouw.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long> {

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    List<BusinessDocument> findByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    List<BusinessDocument> findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByCreatedAtDesc(Long companyId, Long customerId);

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    List<BusinessDocument> findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByCreatedAtDesc(Long companyId, Long projectId);

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    List<BusinessDocument> findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtDesc(Long companyId, Long invoiceId);

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    @Query("select d from BusinessDocument d where d.companyAccount.id = :companyId and d.active = true "
            + "and (:category is null or d.category = :category) "
            + "and (:customerId is null or d.customer.id = :customerId) "
            + "and (:projectId is null or d.project.id = :projectId) "
            + "and (:invoiceId is null or d.invoice.id = :invoiceId) "
            + "and (:query is null or lower(d.title) like lower(concat('%', :query, '%')) "
            + "or lower(d.originalFilename) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(d.notes, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(d.customer.name, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(d.project.name, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(d.invoice.invoiceNumber, '')) like lower(concat('%', :query, '%'))) "
            + "order by d.createdAt desc")
    List<BusinessDocument> searchByCompanyId(@Param("companyId") Long companyId,
                                             @Param("query") String query,
                                             @Param("category") DocumentCategory category,
                                             @Param("customerId") Long customerId,
                                             @Param("projectId") Long projectId,
                                             @Param("invoiceId") Long invoiceId);

    @EntityGraph(attributePaths = {"customer", "project", "invoice"})
    Optional<BusinessDocument> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
