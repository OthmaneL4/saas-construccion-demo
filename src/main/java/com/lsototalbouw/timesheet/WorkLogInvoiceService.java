package com.lsototalbouw.timesheet;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceLine;
import com.lsototalbouw.invoice.InvoiceLineRepository;
import com.lsototalbouw.invoice.InvoiceLineService;
import com.lsototalbouw.invoice.InvoiceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkLogInvoiceService {

    private final WorkLogService workLogService;
    private final WorkLogRepository workLogs;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final InvoiceLineService invoiceLineService;
    private final CompanyContextService companyContext;

    public WorkLogInvoiceService(WorkLogService workLogService, WorkLogRepository workLogs,
                                 InvoiceRepository invoices, InvoiceLineRepository invoiceLines,
                                 InvoiceLineService invoiceLineService, CompanyContextService companyContext) {
        this.workLogService = workLogService;
        this.workLogs = workLogs;
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.invoiceLineService = invoiceLineService;
        this.companyContext = companyContext;
    }

    @Transactional
    public void convertToInvoiceLine(Long workLogId, Long invoiceId) {
        WorkLog workLog = workLogService.getCurrentCompanyWorkLog(workLogId);
        validateConvertible(workLog);
        Invoice invoice = invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        validateInvoiceMatchesWorkLog(workLog, invoice);

        InvoiceLine line = new InvoiceLine(
                invoice,
                descriptionFor(workLog),
                workLog.getHours(),
                workLog.getHourlyRate(),
                BigDecimal.valueOf(21)
        );
        InvoiceLine savedLine = invoiceLines.save(line);
        invoiceLineService.recalculateInvoiceAmount(invoice);
        workLog.markInvoiced(savedLine);
        workLogs.save(workLog);
    }

    private void validateConvertible(WorkLog workLog) {
        if (!workLog.isBillable()) {
            throw new IllegalArgumentException("Este parte no es facturable");
        }
        if (workLog.isInvoiced()) {
            throw new IllegalArgumentException("Este parte ya esta facturado");
        }
        if (workLog.getStatus() != WorkLogStatus.APPROVED) {
            throw new IllegalArgumentException("Solo se pueden facturar partes aprobados");
        }
        if (workLog.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El parte no tiene horas validas");
        }
    }

    private void validateInvoiceMatchesWorkLog(WorkLog workLog, Invoice invoice) {
        if (!invoice.getCustomer().getId().equals(workLog.getProject().getCustomer().getId())) {
            throw new IllegalArgumentException("La factura debe pertenecer al mismo cliente del proyecto");
        }
        if (invoice.getProject() != null && !invoice.getProject().getId().equals(workLog.getProject().getId())) {
            throw new IllegalArgumentException("La factura seleccionada pertenece a otro proyecto");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("No se pueden a horas a una factura pagada");
        }
    }

    private String descriptionFor(WorkLog workLog) {
        return "Horas - " + workLog.getWorkDate() + " - " + workLog.getDescription()
                + " (" + workLog.getWorkerName() + ")";
    }
}
