package com.lsototalbouw.receivable;

import java.math.BigDecimal;

public record ReceivableCustomerRow(
        String customerName,
        long invoiceCount,
        BigDecimal outstandingAmount,
        long maxDaysOverdue
) {
}
