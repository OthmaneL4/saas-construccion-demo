package com.lsototalbouw.quotation;

import com.lsototalbouw.company.CompanyContextService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages quotation line items and keeps quotation totals synchronized.
 *
 * <p>Line totals are the source of truth once at least one active line exists. Any create, update, or archive
 * operation recalculates the parent quotation amount from active lines.
 */
@Service
public class QuotationLineService {

    private final QuotationLineRepository lines;
    private final QuotationService quotationService;
    private final CompanyContextService companyContext;

    public QuotationLineService(QuotationLineRepository lines, QuotationService quotationService,
                                CompanyContextService companyContext) {
        this.lines = lines;
        this.quotationService = quotationService;
        this.companyContext = companyContext;
    }

    /**
     * Adds a line item to a quotation owned by the current company.
     *
     * @param quotationId parent quotation identifier
     * @param form        validated line form
     * @return the persisted quotation line
     */
    @Transactional
    public QuotationLine create(Long quotationId, QuotationLineForm form) {
        Quotation quotation = quotationService.getCurrentCompanyQuotation(quotationId);
        QuotationLine line = new QuotationLine(
                quotation,
                form.getDescription().trim(),
                form.getQuantity(),
                form.getUnitPrice(),
                form.getVatRate()
        );
        QuotationLine saved = lines.save(line);
        recalculateQuotationAmount(quotation);
        return saved;
    }

    /**
     * Loads an active quotation line scoped through its parent quotation and company.
     *
     * @param quotationId parent quotation identifier
     * @param lineId      quotation line identifier
     * @return tenant-scoped quotation line
     */
    @Transactional(readOnly = true)
    public QuotationLine getCurrentCompanyQuotationLine(Long quotationId, Long lineId) {
        return lines.findByQuotationCompanyAccountIdAndQuotationIdAndIdAndActiveTrue(
                        companyContext.currentCompanyId(), quotationId, lineId)
                .orElseThrow(() -> new IllegalArgumentException("Linea de presupuesto no encontrada"));
    }

    /**
     * Updates a quotation line and recalculates the parent quotation amount.
     *
     * @param quotationId parent quotation identifier
     * @param lineId      quotation line identifier
     * @param form        validated line form
     * @return the updated quotation line
     */
    @Transactional
    public QuotationLine update(Long quotationId, Long lineId, QuotationLineForm form) {
        QuotationLine line = getCurrentCompanyQuotationLine(quotationId, lineId);
        line.updateFrom(form.getDescription().trim(), form.getQuantity(), form.getUnitPrice(), form.getVatRate());
        recalculateQuotationAmount(line.getQuotation());
        return line;
    }

    /**
     * Soft-archives a quotation line and recalculates the parent quotation amount.
     *
     * @param quotationId parent quotation identifier
     * @param lineId      quotation line identifier
     */
    @Transactional
    public void archive(Long quotationId, Long lineId) {
        QuotationLine line = getCurrentCompanyQuotationLine(quotationId, lineId);
        line.setActive(false);
        recalculateQuotationAmount(line.getQuotation());
    }

    private void recalculateQuotationAmount(Quotation quotation) {
        if (lines.countByQuotationIdAndActiveTrue(quotation.getId()) == 0) {
            return;
        }
        BigDecimal total = lines.totalByQuotationId(quotation.getId());
        quotation.updateAmountFromLines(total);
    }
}
