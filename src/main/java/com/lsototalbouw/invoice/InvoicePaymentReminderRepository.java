package com.lsototalbouw.invoice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicePaymentReminderRepository extends JpaRepository<InvoicePaymentReminder, Long> {
    List<InvoicePaymentReminder> findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderBySentAtDesc(Long companyId,
                                                                                                         Long invoiceId);
}
