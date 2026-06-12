package com.lsototalbouw.quotation;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.common.enums.QuotationStatus;
import com.lsototalbouw.common.service.BusinessNumberGenerator;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

/**
 * Manages quotation lifecycle operations for the current company.
 *
 * <p>Quotations use tenant-scoped numbering and customer resolution, and can later be converted into invoices
 * by {@link QuotationInvoiceConversionService}. This service owns header-level validation and status changes,
 * while line totals are recalculated by {@link QuotationLineService}.
 */
@Service
public class QuotationService {

    private final QuotationRepository quotations;
    private final CustomerRepository customers;
    private final CompanyContextService companyContext;
    private final BusinessNumberGenerator numberGenerator;

    public QuotationService(QuotationRepository quotations, CustomerRepository customers,
                            CompanyContextService companyContext, BusinessNumberGenerator numberGenerator) {
        this.quotations = quotations;
        this.customers = customers;
        this.companyContext = companyContext;
        this.numberGenerator = numberGenerator;
    }

    /**
     * Builds a new quotation form with generated number and default validity window.
     *
     * @return pre-populated quotation form for the current company
     */
    @Transactional(readOnly = true)
    public QuotationForm newQuotationForm() {
        QuotationForm form = new QuotationForm();
        form.setQuotationNumber(nextQuotationNumber());
        form.setIssueDate(LocalDate.now());
        form.setExpiryDate(LocalDate.now().plusDays(30));
        return form;
    }

    /**
     * Generates the next quotation number for the current company.
     *
     * @return the next available quotation number using the configured quotation prefix
     */
    @Transactional(readOnly = true)
    public String nextQuotationNumber() {
        Long companyId = companyContext.currentCompanyId();
        return numberGenerator.nextNumber("OFF", quotations.findQuotationNumbersByCompanyId(companyId));
    }

    /**
     * Validates quotation numbering uniqueness within the active company.
     *
     * @param form               submitted quotation form
     * @param currentQuotationId optional quotation identifier to exclude during updates
     * @param bindingResult      validation sink used by the MVC controller
     */
    public void validate(QuotationForm form, Long currentQuotationId, BindingResult bindingResult) {
        Long companyId = companyContext.currentCompanyId();
        if (form.getQuotationNumber() == null) {
            return;
        }
        String quotationNumber = form.getQuotationNumber().trim();
        boolean duplicate = currentQuotationId == null
                ? quotations.existsByCompanyAccountIdAndQuotationNumberIgnoreCase(companyId, quotationNumber)
                : quotations.existsByCompanyAccountIdAndQuotationNumberIgnoreCaseAndIdNot(companyId, quotationNumber, currentQuotationId);
        if (duplicate) {
            bindingResult.rejectValue("quotationNumber", "quotation.number.duplicate",
                    "Ya existe un presupuesto con este numero");
        }
    }

    /**
     * Loads an active quotation belonging to the current company.
     *
     * @param id quotation identifier from the route
     * @return tenant-scoped quotation
     * @throws IllegalArgumentException when the quotation is missing, archived, or belongs to another company
     */
    @Transactional(readOnly = true)
    public Quotation getCurrentCompanyQuotation(Long id) {
        return quotations.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado"));
    }

    /**
     * Creates a quotation for a customer in the current company.
     *
     * @param form validated quotation form
     * @return the persisted quotation
     */
    @Transactional
    public Quotation create(QuotationForm form) {
        Long companyId = companyContext.currentCompanyId();
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        Quotation quotation = new Quotation(
                customer.getCompanyAccount(),
                customer,
                form.getQuotationNumber().trim(),
                form.getTitle().trim(),
                clean(form.getDescription()),
                form.getAmount(),
                form.getIssueDate(),
                form.getExpiryDate(),
                form.getStatus()
        );
        return quotations.save(quotation);
    }

    /**
     * Updates quotation header data while preserving tenant boundaries.
     *
     * @param id   quotation identifier
     * @param form validated quotation form
     * @return the updated quotation
     */
    @Transactional
    public Quotation update(Long id, QuotationForm form) {
        Long companyId = companyContext.currentCompanyId();
        Quotation quotation = getCurrentCompanyQuotation(id);
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        quotation.updateFrom(
                customer,
                form.getQuotationNumber().trim(),
                form.getTitle().trim(),
                clean(form.getDescription()),
                form.getAmount(),
                form.getIssueDate(),
                form.getExpiryDate(),
                form.getStatus()
        );
        return quotation;
    }

    /**
     * Updates the business status of a quotation.
     *
     * @param id     quotation identifier
     * @param status target status selected by the user
     * @return the updated quotation
     */
    @Transactional
    public Quotation updateStatus(Long id, QuotationStatus status) {
        Quotation quotation = getCurrentCompanyQuotation(id);
        quotation.updateStatus(status);
        return quotation;
    }

    /**
     * Soft-archives a quotation without deleting its historical record.
     *
     * @param id quotation identifier
     */
    @Transactional
    public void archive(Long id) {
        Quotation quotation = getCurrentCompanyQuotation(id);
        quotation.setActive(false);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
