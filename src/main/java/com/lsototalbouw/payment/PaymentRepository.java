package com.lsototalbouw.payment;

import com.lsototalbouw.common.enums.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    List<Payment> findByCompanyAccountIdAndActiveTrueOrderByPaymentDateDesc(Long companyId);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    List<Payment> findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(Long companyId, Long invoiceId);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    @Query("""
            select payment
            from Payment payment
            join payment.invoice invoice
            join invoice.customer customer
            where payment.companyAccount.id = :companyId
              and payment.active = true
              and (:invoiceId is null or invoice.id = :invoiceId)
              and (:status is null or payment.status = :status)
              and (:fromDate is null or payment.paymentDate >= :fromDate)
              and (:toDate is null or payment.paymentDate <= :toDate)
              and (
                    :query is null
                 or lower(invoice.invoiceNumber) like lower(concat('%', :query, '%'))
                 or lower(customer.name) like lower(concat('%', :query, '%'))
                 or lower(payment.method) like lower(concat('%', :query, '%'))
              )
            order by payment.paymentDate desc
            """)
    List<Payment> searchByCompanyId(@Param("companyId") Long companyId,
                                    @Param("invoiceId") Long invoiceId,
                                    @Param("status") PaymentStatus status,
                                    @Param("query") String query,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    Optional<Payment> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
