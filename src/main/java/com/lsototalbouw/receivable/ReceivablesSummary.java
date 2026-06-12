package com.lsototalbouw.receivable;

import java.math.BigDecimal;

public record ReceivablesSummary(
        BigDecimal totalOutstanding,
        BigDecimal overdueOutstanding,
        BigDecimal dueSoonOutstanding,
        long openInvoiceCount,
        long overdueInvoiceCount
) {
}
