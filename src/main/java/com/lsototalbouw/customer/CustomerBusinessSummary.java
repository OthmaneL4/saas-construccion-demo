package com.lsototalbouw.customer;

import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.project.Project;
import java.math.BigDecimal;
import java.util.List;

public record CustomerBusinessSummary(
        int projectCount,
        int invoiceCount,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingAmount
) {

    public static CustomerBusinessSummary from(List<Project> projects, List<Invoice> invoices) {
        BigDecimal totalInvoiced = invoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = invoices.stream()
                .map(Invoice::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerBusinessSummary(
                projects.size(),
                invoices.size(),
                totalInvoiced,
                totalPaid,
                totalInvoiced.subtract(totalPaid)
        );
    }
}
