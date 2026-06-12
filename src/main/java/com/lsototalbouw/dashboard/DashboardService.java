package com.lsototalbouw.dashboard;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.common.enums.ProjectStatus;
import com.lsototalbouw.common.enums.ToolStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.material.MaterialRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.tool.ToolRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final int DUE_SOON_DAYS = 7;
    private static final List<InvoiceStatus> OPEN_INVOICE_STATUSES = List.of(
            InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE);

    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;
    private final MaterialRepository materials;
    private final ToolRepository tools;
    private final CompanyContextService companyContext;

    public DashboardService(CustomerRepository customers, ProjectRepository projects, InvoiceRepository invoices,
                            ExpenseRepository expenses, MaterialRepository materials, ToolRepository tools,
                            CompanyContextService companyContext) {
        this.customers = customers;
        this.projects = projects;
        this.invoices = invoices;
        this.expenses = expenses;
        this.materials = materials;
        this.tools = tools;
        this.companyContext = companyContext;
    }

    public DashboardSummary getSummary() {
        Long companyId = companyContext.currentCompanyId();
        List<Invoice> openInvoices = openInvoicesForCurrentCompany();
        LocalDate today = LocalDate.now();
        BigDecimal totalInvoiced = invoices.totalInvoicedByCompanyId(companyId);
        BigDecimal totalPaid = invoices.totalPaidByCompanyId(companyId);
        BigDecimal totalExpenses = expenses.totalExpensesByCompanyId(companyId);
        BigDecimal totalOutstanding = openInvoices.stream()
                .map(invoice -> invoice.getAmount().subtract(invoice.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummary(
                customers.countByCompanyAccountIdAndActiveTrue(companyId),
                projects.countByCompanyAccountIdAndStatusAndActiveTrue(companyId, ProjectStatus.IN_PROGRESS),
                openInvoices.size(),
                totalInvoiced,
                totalPaid,
                totalExpenses,
                totalPaid.subtract(totalExpenses),
                totalOutstanding,
                openInvoices.stream()
                        .filter(invoice -> isOverdue(invoice, today))
                        .count(),
                openInvoices.stream()
                        .filter(invoice -> isDueSoon(invoice, today))
                        .count(),
                materials.countLowStockByCompanyId(companyId),
                tools.countByCompanyAccountIdAndStatusAndActiveTrue(companyId, ToolStatus.MAINTENANCE)
        );
    }

    public List<Invoice> openInvoicesForCurrentCompany() {
        return invoices.findOpenByCompanyId(companyContext.currentCompanyId(), OPEN_INVOICE_STATUSES);
    }

    private boolean isOverdue(Invoice invoice, LocalDate today) {
        return invoice.getStatus() == InvoiceStatus.OVERDUE
                || invoice.getDueDate() != null && invoice.getDueDate().isBefore(today);
    }

    private boolean isDueSoon(Invoice invoice, LocalDate today) {
        return invoice.getDueDate() != null
                && !invoice.getDueDate().isBefore(today)
                && !invoice.getDueDate().isAfter(today.plusDays(DUE_SOON_DAYS));
    }
}
