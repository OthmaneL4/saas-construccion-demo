package com.lsototalbouw.receivable;

import java.util.List;

public record ReceivablesView(
        ReceivablesSummary summary,
        List<ReceivableInvoiceRow> invoices,
        List<ReceivableCustomerRow> customers
) {
}
