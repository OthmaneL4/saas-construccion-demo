package com.lsototalbouw.quotation;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceLine;
import com.lsototalbouw.invoice.InvoiceLineRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.invoice.InvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converts accepted quotations into invoices while preserving line-item financial detail.
 *
 * <p>The conversion is tenant-scoped and idempotency-protected: a quotation can only create one active invoice.
 * All active quotation lines are copied into invoice lines, the invoice amount is recalculated from those lines,
 * and the quotation is marked accepted once conversion succeeds.
 */
@Service
public class QuotationInvoiceConversionService {

    private final QuotationService quotationService;
    private final QuotationLineRepository quotationLines;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final InvoiceService invoiceService;
    private final CompanyContextService companyContext;

    public QuotationInvoiceConversionService(QuotationService quotationService,
                                             QuotationLineRepository quotationLines,
                                             InvoiceRepository invoices,
                                             InvoiceLineRepository invoiceLines,
                                             InvoiceService invoiceService,
                                             CompanyContextService companyContext) {
        this.quotationService = quotationService;
        this.quotationLines = quotationLines;
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.invoiceService = invoiceService;
        this.companyContext = companyContext;
    }

    /**
     * Converts a quotation into a sent invoice for the current company.
     *
     * @param quotationId quotation identifier from the route
     * @return the newly created invoice
     * @throws IllegalStateException when the quotation already has an active converted invoice
     */
    @Transactional
    public Invoice convertToInvoice(Long quotationId) {
        Quotation quotation = quotationService.getCurrentCompanyQuotation(quotationId);
        Long companyId = companyContext.currentCompanyId();
        invoices.findByCompanyAccountIdAndQuotationIdAndActiveTrue(companyId, quotationId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Este presupuesto ya fue convertido en la factura "
                            + existing.getInvoiceNumber() + ".");
                });

        LocalDate issueDate = LocalDate.now();
        Invoice invoice = invoices.save(new Invoice(
                quotation.getCompanyAccount(),
                quotation.getCustomer(),
                null,
                invoiceService.nextInvoiceNumber(),
                quotation.getAmount(),
                BigDecimal.ZERO,
                issueDate,
                issueDate.plusDays(companyContext.currentCompany().getPaymentTermsDays()),
                InvoiceStatus.SENT,
                quotation
        ));

        List<QuotationLine> lines = quotationLines
                .findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(
                        companyId, quotationId);
        for (QuotationLine line : lines) {
            invoiceLines.save(new InvoiceLine(
                    invoice,
                    line.getDescription(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getVatRate()
            ));
        }
        if (!lines.isEmpty()) {
            invoice.updateAmountFromLines(invoiceLines.totalByInvoiceId(invoice.getId()));
        }
        quotation.markAccepted();
        return invoice;
    }

    /**
     * Finds the active invoice that was created from a quotation, if any.
     *
     * @param quotationId quotation identifier
     * @return optional converted invoice scoped to the current company
     */
    @Transactional(readOnly = true)
    public Optional<Invoice> convertedInvoice(Long quotationId) {
        return invoices.findByCompanyAccountIdAndQuotationIdAndActiveTrue(companyContext.currentCompanyId(), quotationId);
    }
}
