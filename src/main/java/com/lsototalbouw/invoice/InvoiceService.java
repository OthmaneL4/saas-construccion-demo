package com.lsototalbouw.invoice;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.common.service.BusinessNumberGenerator;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

/**
 * Manages invoice creation, validation, updates, and payment reminder state for the active company.
 *
 * <p>The service centralizes invoice numbering, due-date defaults, duplicate protection, tenant-scoped
 * entity resolution, and basic financial consistency checks. Line-level recalculation is handled by
 * {@link InvoiceLineService}, while payment balance changes are coordinated by the payment module.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;
    private final BusinessNumberGenerator numberGenerator;

    public InvoiceService(InvoiceRepository invoices, CustomerRepository customers,
                          ProjectRepository projects, CompanyContextService companyContext,
                          BusinessNumberGenerator numberGenerator) {
        this.invoices = invoices;
        this.customers = customers;
        this.projects = projects;
        this.companyContext = companyContext;
        this.numberGenerator = numberGenerator;
    }

    /**
     * Validates a new invoice form before persistence.
     *
     * @param form          submitted invoice form
     * @param bindingResult validation sink used by the MVC controller
     */
    public void validate(InvoiceForm form, BindingResult bindingResult) {
        validate(form, null, bindingResult);
    }

    /**
     * Builds a new invoice form with a tenant-scoped generated number and default due date.
     *
     * @return pre-populated invoice form for the current company
     */
    @Transactional(readOnly = true)
    public InvoiceForm newInvoiceForm() {
        CompanyAccount company = companyContext.currentCompany();
        InvoiceForm form = new InvoiceForm();
        form.setInvoiceNumber(nextInvoiceNumber());
        form.setIssueDate(LocalDate.now());
        form.setDueDate(form.getIssueDate().plusDays(company.getPaymentTermsDays()));
        return form;
    }

    /**
     * Generates the next invoice number for the current company.
     *
     * @return the next available invoice number using the configured invoice prefix
     */
    @Transactional(readOnly = true)
    public String nextInvoiceNumber() {
        Long companyId = companyContext.currentCompanyId();
        return numberGenerator.nextNumber("INV", invoices.findInvoiceNumbersByCompanyId(companyId));
    }

    /**
     * Validates an invoice form and protects against duplicate numbers inside the same company.
     *
     * @param form             submitted invoice form
     * @param currentInvoiceId optional invoice identifier to exclude during updates
     * @param bindingResult    validation sink used by the MVC controller
     */
    public void validate(InvoiceForm form, Long currentInvoiceId, BindingResult bindingResult) {
        Long companyId = companyContext.currentCompanyId();
        if (form.getPaidAmount() != null && form.getAmount() != null
                && form.getPaidAmount().compareTo(form.getAmount()) > 0) {
            bindingResult.rejectValue("paidAmount", "invoice.paidAmount.exceedsAmount",
                    "El importe cobrado no puede superar el importe total");
        }
        if (form.getInvoiceNumber() == null) {
            return;
        }

        String invoiceNumber = form.getInvoiceNumber().trim();
        boolean duplicate = currentInvoiceId == null
                ? invoices.existsByCompanyAccountIdAndInvoiceNumberIgnoreCase(companyId, invoiceNumber)
                : invoices.existsByCompanyAccountIdAndInvoiceNumberIgnoreCaseAndIdNot(companyId, invoiceNumber, currentInvoiceId);
        if (duplicate) {
            bindingResult.rejectValue("invoiceNumber", "invoice.invoiceNumber.duplicate",
                    "Ya existe una factura con este numero");
        }
    }

    /**
     * Creates an invoice for a customer and optional project in the current company.
     *
     * @param form validated invoice form
     * @return the persisted invoice
     */
    @Transactional
    public Invoice create(InvoiceForm form) {
        applyDefaultDueDate(form);
        Long companyId = companyContext.currentCompanyId();
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Project project = null;
        if (form.getProjectId() != null) {
            project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        }

        Invoice invoice = new Invoice(
                customer.getCompanyAccount(),
                customer,
                project,
                form.getInvoiceNumber().trim(),
                form.getAmount(),
                form.getPaidAmount() == null ? BigDecimal.ZERO : form.getPaidAmount(),
                form.getIssueDate(),
                form.getDueDate(),
                form.getStatus()
        );
        return invoices.save(invoice);
    }

    /**
     * Loads an active invoice belonging to the current company.
     *
     * @param id invoice identifier from the route
     * @return tenant-scoped invoice
     * @throws IllegalArgumentException when the invoice is missing, archived, or belongs to another company
     */
    @Transactional(readOnly = true)
    public Invoice getCurrentCompanyInvoice(Long id) {
        return invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
    }

    /**
     * Updates invoice header data while preserving tenant boundaries.
     *
     * @param id   invoice identifier
     * @param form validated invoice form
     * @return the updated invoice
     */
    @Transactional
    public Invoice update(Long id, InvoiceForm form) {
        applyDefaultDueDate(form);
        Long companyId = companyContext.currentCompanyId();
        Invoice invoice = getCurrentCompanyInvoice(id);
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Project project = null;
        if (form.getProjectId() != null) {
            project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        }

        invoice.updateFrom(
                customer,
                project,
                form.getInvoiceNumber().trim(),
                form.getAmount(),
                form.getPaidAmount() == null ? BigDecimal.ZERO : form.getPaidAmount(),
                form.getIssueDate(),
                form.getDueDate(),
                form.getStatus()
        );
        return invoice;
    }

    /**
     * Soft-archives an invoice without deleting its financial history.
     *
     * @param id invoice identifier
     */
    @Transactional
    public void archive(Long id) {
        Invoice invoice = getCurrentCompanyInvoice(id);
        invoice.setActive(false);
    }

    /**
     * Records that a payment reminder has been sent for an invoice.
     *
     * @param id invoice identifier
     * @return the updated invoice
     * @throws IllegalStateException when reminders are not allowed for the invoice status
     */
    @Transactional
    public Invoice markPaymentReminderSent(Long id) {
        Invoice invoice = getCurrentCompanyInvoice(id);
        if (!invoice.isPaymentReminderAllowed()) {
            throw new IllegalStateException("No se puede reclamar una factura pagada o cancelada.");
        }
        invoice.markPaymentReminderSent(LocalDateTime.now());
        return invoice;
    }

    /**
     * Returns the default payment terms configured for the current company.
     *
     * @return number of calendar days between invoice issue date and due date
     */
    @Transactional(readOnly = true)
    public int currentCompanyPaymentTermsDays() {
        return companyContext.currentCompany().getPaymentTermsDays();
    }

    private void applyDefaultDueDate(InvoiceForm form) {
        if (form.getIssueDate() == null) {
            form.setIssueDate(LocalDate.now());
        }
        if (form.getDueDate() == null) {
            form.setDueDate(form.getIssueDate().plusDays(companyContext.currentCompany().getPaymentTermsDays()));
        }
    }
}
