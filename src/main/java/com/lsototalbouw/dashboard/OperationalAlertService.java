package com.lsototalbouw.dashboard;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.material.MaterialItem;
import com.lsototalbouw.material.MaterialRepository;
import com.lsototalbouw.tool.ToolItem;
import com.lsototalbouw.tool.ToolRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalAlertService {

    private static final int MAX_ALERTS_PER_TYPE = 3;
    private static final int MAX_TOTAL_ALERTS = 9;
    private static final int INVOICE_DUE_SOON_DAYS = 7;
    private static final int TOOL_MAINTENANCE_DAYS = 14;

    private final CompanyContextService companyContext;
    private final InvoiceRepository invoices;
    private final MaterialRepository materials;
    private final ToolRepository tools;

    public OperationalAlertService(CompanyContextService companyContext, InvoiceRepository invoices,
                                   MaterialRepository materials, ToolRepository tools) {
        this.companyContext = companyContext;
        this.invoices = invoices;
        this.materials = materials;
        this.tools = tools;
    }

    @Transactional(readOnly = true)
    public List<OperationalAlert> currentAlerts() {
        Long companyId = companyContext.currentCompanyId();
        LocalDate today = LocalDate.now();
        List<OperationalAlert> alerts = new ArrayList<>();

        alerts.addAll(invoiceAlerts(companyId, today));
        alerts.addAll(materialAlerts(companyId));
        alerts.addAll(toolAlerts(companyId, today));

        return alerts.stream()
                .sorted(Comparator.comparingInt(OperationalAlert::priority)
                        .thenComparing(alert -> alert.dueDate() == null ? LocalDate.MAX : alert.dueDate()))
                .limit(MAX_TOTAL_ALERTS)
                .toList();
    }

    private List<OperationalAlert> invoiceAlerts(Long companyId, LocalDate today) {
        LocalDate dueSoonLimit = today.plusDays(INVOICE_DUE_SOON_DAYS);
        List<Invoice> openInvoices = invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyId).stream()
                .filter(this::isOpenInvoice)
                .filter(invoice -> invoice.getDueDate() != null)
                .toList();

        List<OperationalAlert> overdue = openInvoices.stream()
                .filter(invoice -> invoice.getDueDate().isBefore(today) || invoice.getStatus() == InvoiceStatus.OVERDUE)
                .limit(MAX_ALERTS_PER_TYPE)
                .map(invoice -> invoiceAlert(invoice, "danger", "danger", "fa-circle-exclamation",
                        "Factura vencida", 10))
                .toList();

        List<OperationalAlert> dueSoon = openInvoices.stream()
                .filter(invoice -> !invoice.getDueDate().isBefore(today))
                .filter(invoice -> !invoice.getDueDate().isAfter(dueSoonLimit))
                .limit(MAX_ALERTS_PER_TYPE)
                .map(invoice -> invoiceAlert(invoice, "warning", "warning", "fa-clock",
                        "Factura proxima a vencer", 20))
                .toList();

        List<OperationalAlert> alerts = new ArrayList<>(overdue);
        alerts.addAll(dueSoon);
        return alerts;
    }

    private OperationalAlert invoiceAlert(Invoice invoice, String severity, String statusClass, String icon,
                                          String title, int priority) {
        BigDecimal outstanding = invoice.getAmount().subtract(invoice.getPaidAmount());
        String customerName = invoice.getCustomer().getName();
        String message = invoice.getInvoiceNumber() + " - " + customerName
                + " - pendiente EUR " + outstanding.toPlainString();
        String sourceKey = "invoice:" + invoice.getId() + ":" + title.toLowerCase().replace(' ', '-');
        return new OperationalAlert(sourceKey, severity, statusClass, icon, title, message,
                "/invoices/" + invoice.getId(), invoice.getDueDate(), priority);
    }

    private List<OperationalAlert> materialAlerts(Long companyId) {
        return materials.searchByCompanyId(companyId, null, true).stream()
                .limit(MAX_ALERTS_PER_TYPE)
                .map(material -> new OperationalAlert("material:" + material.getId() + ":low-stock",
                        "warning", "warning", "fa-boxes-stacked",
                        "Stock bajo", materialMessage(material), "/materials",
                        null, 30))
                .toList();
    }

    private String materialMessage(MaterialItem material) {
        return material.getName() + " - stock " + material.getStockQuantity()
                + " " + material.getUnit() + " / minimo " + material.getMinimumStock();
    }

    private List<OperationalAlert> toolAlerts(Long companyId, LocalDate today) {
        LocalDate maintenanceLimit = today.plusDays(TOOL_MAINTENANCE_DAYS);
        return tools.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId).stream()
                .filter(tool -> tool.getNextMaintenanceDate() != null)
                .filter(tool -> !tool.getNextMaintenanceDate().isAfter(maintenanceLimit))
                .limit(MAX_ALERTS_PER_TYPE)
                .map(tool -> toolAlert(tool, today))
                .toList();
    }

    private OperationalAlert toolAlert(ToolItem tool, LocalDate today) {
        boolean overdue = tool.getNextMaintenanceDate().isBefore(today);
        return new OperationalAlert("tool:" + tool.getId() + ":maintenance",
                overdue ? "danger" : "info",
                overdue ? "danger" : "info",
                overdue ? "fa-screwdriver-wrench" : "fa-calendar-check",
                overdue ? "Mantenimiento vencido" : "Mantenimiento cercano",
                tool.getName() + " - fecha " + tool.getNextMaintenanceDate(),
                "/tools", tool.getNextMaintenanceDate(), overdue ? 15 : 40);
    }

    private boolean isOpenInvoice(Invoice invoice) {
        return invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.CANCELLED;
    }
}
