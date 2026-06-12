package com.lsototalbouw.receivable;

import com.lsototalbouw.common.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableInvoiceRow(
        Long invoiceId,
        String invoiceNumber,
        String customerName,
        String projectName,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate dueDate,
        long daysOverdue,
        InvoiceStatus status,
        int paymentReminderCount,
        String priority,
        String statusClass
) {
}
