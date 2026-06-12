package com.lsototalbouw.invoice;

import com.lsototalbouw.company.CompanyContextService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages invoice line items and keeps invoice totals synchronized.
 *
 * <p>Any line create, update, or archive operation recalculates the parent invoice amount from active lines.
 * This prevents stale invoice totals when users edit quantity, unit price, VAT rate, or line availability.
 */
@Service
public class InvoiceLineService {

    private final InvoiceLineRepository lines;
    private final InvoiceService invoiceService;
    private final CompanyContextService companyContext;

    public InvoiceLineService(InvoiceLineRepository lines, InvoiceService invoiceService,
                              CompanyContextService companyContext) {
        this.lines = lines;
        this.invoiceService = invoiceService;
        this.companyContext = companyContext;
    }

    /**
     * Adds a line item to an invoice owned by the current company.
     *
     * @param invoiceId parent invoice identifier
     * @param form      validated line form
     * @return the persisted invoice line
     */
    @Transactional
    public InvoiceLine create(Long invoiceId, InvoiceLineForm form) {
        Invoice invoice = invoiceService.getCurrentCompanyInvoice(invoiceId);
        InvoiceLine line = new InvoiceLine(
                invoice,
                form.getDescription().trim(),
                form.getQuantity(),
                form.getUnitPrice(),
                form.getVatRate()
        );
        InvoiceLine saved = lines.save(line);
        recalculateInvoiceAmount(invoice);
        return saved;
    }

    /**
     * Loads an active invoice line scoped through its parent invoice and company.
     *
     * @param invoiceId parent invoice identifier
     * @param lineId    invoice line identifier
     * @return tenant-scoped invoice line
     */
    @Transactional(readOnly = true)
    public InvoiceLine getCurrentCompanyInvoiceLine(Long invoiceId, Long lineId) {
        return lines.findByInvoiceCompanyAccountIdAndInvoiceIdAndIdAndActiveTrue(
                        companyContext.currentCompanyId(), invoiceId, lineId)
                .orElseThrow(() -> new IllegalArgumentException("Linea de factura no encontrada"));
    }

    /**
     * Updates an invoice line and recalculates the parent invoice amount.
     *
     * @param invoiceId parent invoice identifier
     * @param lineId    invoice line identifier
     * @param form      validated line form
     * @return the updated invoice line
     */
    @Transactional
    public InvoiceLine update(Long invoiceId, Long lineId, InvoiceLineForm form) {
        InvoiceLine line = getCurrentCompanyInvoiceLine(invoiceId, lineId);
        line.updateFrom(form.getDescription().trim(), form.getQuantity(), form.getUnitPrice(), form.getVatRate());
        recalculateInvoiceAmount(line.getInvoice());
        return line;
    }

    /**
     * Soft-archives an invoice line and recalculates the parent invoice amount.
     *
     * @param invoiceId parent invoice identifier
     * @param lineId    invoice line identifier
     */
    @Transactional
    public void archive(Long invoiceId, Long lineId) {
        InvoiceLine line = getCurrentCompanyInvoiceLine(invoiceId, lineId);
        line.setActive(false);
        recalculateInvoiceAmount(line.getInvoice());
    }

    /**
     * Recalculates an invoice amount from its active lines.
     *
     * @param invoice invoice whose amount should be synchronized
     */
    @Transactional
    public void recalculateInvoiceAmount(Invoice invoice) {
        if (lines.countByInvoiceIdAndActiveTrue(invoice.getId()) == 0) {
            return;
        }
        BigDecimal total = lines.totalByInvoiceId(invoice.getId());
        invoice.updateAmountFromLines(total);
    }
}
