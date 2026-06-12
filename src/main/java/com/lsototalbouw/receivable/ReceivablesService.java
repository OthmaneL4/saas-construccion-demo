package com.lsototalbouw.receivable;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the receivables workspace for the currently authenticated company.
 *
 * <p>The service derives operational payment priorities from invoice state, due dates, reminder count,
 * and outstanding amount. It keeps all filtering tenant-scoped through {@link CompanyContextService}
 * so finance users only see invoices that belong to their active company account.
 */
@Service
public class ReceivablesService {

    private static final int DUE_SOON_DAYS = 7;
    public static final List<String> PRIORITY_OPTIONS = List.of("Alta", "Media", "Proxima", "Normal");

    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;

    public ReceivablesService(InvoiceRepository invoices, CompanyContextService companyContext) {
        this.invoices = invoices;
        this.companyContext = companyContext;
    }

    /**
     * Returns the unfiltered receivables view for the current company.
     *
     * @return a dashboard-ready view containing summary metrics, invoice rows, and customer exposure rows
     */
    @Transactional(readOnly = true)
    public ReceivablesView currentCompanyReceivables() {
        return currentCompanyReceivables(null, null);
    }

    /**
     * Returns the receivables view for the current company after applying safe query and priority filters.
     *
     * <p>Only open invoices with a positive outstanding amount are included. Results are sorted by operational
     * priority first, then due date, then outstanding amount so the most urgent collection actions appear first.
     *
     * @param query    optional case-insensitive search text for invoice number, customer, or project
     * @param priority optional priority filter; invalid values are ignored
     * @return a filtered receivables view scoped to the current company
     */
    @Transactional(readOnly = true)
    public ReceivablesView currentCompanyReceivables(String query, String priority) {
        LocalDate today = LocalDate.now();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        String cleanPriority = cleanPriority(priority);
        List<ReceivableInvoiceRow> rows = invoices
                .findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyContext.currentCompanyId()).stream()
                .filter(this::isOpenInvoice)
                .map(invoice -> toRow(invoice, today))
                .filter(row -> row.outstandingAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(row -> matchesQuery(row, cleanQuery))
                .filter(row -> matchesPriority(row, cleanPriority))
                .sorted(Comparator.comparingInt(this::priorityRank)
                        .thenComparing(ReceivableInvoiceRow::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReceivableInvoiceRow::outstandingAmount, Comparator.reverseOrder()))
                .toList();

        return new ReceivablesView(summary(rows, today), rows, customerRows(rows));
    }

    private ReceivableInvoiceRow toRow(Invoice invoice, LocalDate today) {
        BigDecimal outstanding = invoice.getAmount().subtract(invoice.getPaidAmount());
        long daysOverdue = invoice.getDueDate() != null && invoice.getDueDate().isBefore(today)
                ? ChronoUnit.DAYS.between(invoice.getDueDate(), today)
                : 0;
        String priority = priority(invoice, today, daysOverdue);
        return new ReceivableInvoiceRow(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getProject() == null ? null : invoice.getProject().getName(),
                invoice.getAmount(),
                invoice.getPaidAmount(),
                outstanding,
                invoice.getDueDate(),
                daysOverdue,
                invoice.getStatus(),
                invoice.getPaymentReminderCount(),
                priority,
                statusClass(priority)
        );
    }

    private ReceivablesSummary summary(List<ReceivableInvoiceRow> rows, LocalDate today) {
        BigDecimal totalOutstanding = sum(rows);
        BigDecimal overdueOutstanding = sum(rows.stream()
                .filter(row -> row.daysOverdue() > 0 || row.status() == InvoiceStatus.OVERDUE)
                .toList());
        BigDecimal dueSoonOutstanding = sum(rows.stream()
                .filter(row -> row.daysOverdue() == 0)
                .filter(row -> row.dueDate() != null && !row.dueDate().isAfter(today.plusDays(DUE_SOON_DAYS)))
                .toList());
        long overdueCount = rows.stream()
                .filter(row -> row.daysOverdue() > 0 || row.status() == InvoiceStatus.OVERDUE)
                .count();
        return new ReceivablesSummary(totalOutstanding, overdueOutstanding, dueSoonOutstanding,
                rows.size(), overdueCount);
    }

    private List<ReceivableCustomerRow> customerRows(List<ReceivableInvoiceRow> rows) {
        Map<String, List<ReceivableInvoiceRow>> byCustomer = new LinkedHashMap<>();
        for (ReceivableInvoiceRow row : rows) {
            byCustomer.computeIfAbsent(row.customerName(), key -> new java.util.ArrayList<>()).add(row);
        }
        return byCustomer.entrySet().stream()
                .map(entry -> new ReceivableCustomerRow(
                        entry.getKey(),
                        entry.getValue().size(),
                        sum(entry.getValue()),
                        entry.getValue().stream().mapToLong(ReceivableInvoiceRow::daysOverdue).max().orElse(0)))
                .sorted(Comparator.comparing(ReceivableCustomerRow::outstandingAmount, Comparator.reverseOrder()))
                .toList();
    }

    private BigDecimal sum(List<ReceivableInvoiceRow> rows) {
        return rows.stream()
                .map(ReceivableInvoiceRow::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String priority(Invoice invoice, LocalDate today, long daysOverdue) {
        if (daysOverdue >= 14 || invoice.getStatus() == InvoiceStatus.OVERDUE) {
            return "Alta";
        }
        if (daysOverdue > 0 || invoice.getPaymentReminderCount() > 0) {
            return "Media";
        }
        if (invoice.getDueDate() != null && !invoice.getDueDate().isAfter(today.plusDays(DUE_SOON_DAYS))) {
            return "Proxima";
        }
        return "Normal";
    }

    private String statusClass(String priority) {
        return switch (priority) {
            case "Alta" -> "danger";
            case "Media", "Proxima" -> "warning";
            default -> "info";
        };
    }

    private int priorityRank(ReceivableInvoiceRow row) {
        return switch (row.priority()) {
            case "Alta" -> 0;
            case "Media" -> 1;
            case "Proxima" -> 2;
            default -> 3;
        };
    }

    private boolean isOpenInvoice(Invoice invoice) {
        return invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.CANCELLED;
    }

    private boolean matchesQuery(ReceivableInvoiceRow row, String query) {
        if (query == null) {
            return true;
        }
        return contains(row.invoiceNumber(), query)
                || contains(row.customerName(), query)
                || contains(row.projectName(), query);
    }

    private boolean matchesPriority(ReceivableInvoiceRow row, String priority) {
        return priority == null || row.priority().equals(priority);
    }

    /**
     * Normalizes a user-provided priority filter to one of the supported values.
     *
     * @param priority raw priority value from the UI
     * @return the accepted priority label, or {@code null} when the value is blank or unsupported
     */
    public String cleanPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        String cleanPriority = priority.trim();
        return PRIORITY_OPTIONS.contains(cleanPriority) ? cleanPriority : null;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
