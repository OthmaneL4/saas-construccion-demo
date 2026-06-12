package com.lsototalbouw.dashboard;

import java.math.BigDecimal;

public record DashboardSummary(
        long customers,
        long activeProjects,
        long pendingInvoices,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalExpenses,
        BigDecimal netResult,
        BigDecimal totalOutstanding,
        long overdueInvoices,
        long dueSoonInvoices,
        long lowStockMaterials,
        long toolsInMaintenance
) {
}
