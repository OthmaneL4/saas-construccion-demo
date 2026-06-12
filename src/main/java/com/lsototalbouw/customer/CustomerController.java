package com.lsototalbouw.customer;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.document.BusinessDocument;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.document.DocumentService;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.quotation.QuotationRepository;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

    private final CustomerRepository customers;
    private final CustomerService customerService;
    private final CompanyContextService companyContext;
    private final BusinessDocumentRepository documents;
    private final DocumentService documentService;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final QuotationRepository quotations;
    private final AuditService auditService;

    public CustomerController(CustomerRepository customers, CustomerService customerService,
                               CompanyContextService companyContext, BusinessDocumentRepository documents,
                               DocumentService documentService, ProjectRepository projects, InvoiceRepository invoices,
                               QuotationRepository quotations, AuditService auditService) {
        this.customers = customers;
        this.customerService = customerService;
        this.companyContext = companyContext;
        this.documents = documents;
        this.documentService = documentService;
        this.projects = projects;
        this.invoices = invoices;
        this.quotations = quotations;
        this.auditService = auditService;
    }

    @GetMapping("/customers")
    public String index(Model model, @ModelAttribute("customerForm") CustomerForm customerForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "page", defaultValue = "0") int page) {
        addIndexData(model, query, page);
        model.addAttribute("customerForm", customerForm);
        return "customers/index";
    }

    @PostMapping("/customers")
    public String create(@Valid @ModelAttribute("customerForm") CustomerForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, 0);
            return "customers/index";
        }
        Customer customer = customerService.create(form);
        auditService.record(AuditAction.CREATE, "Clientes", customer.getId(),
                "Cliente creado: " + customer.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Cliente creado correctamente.");
        return "redirect:/customers";
    }

    @GetMapping("/customers/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        var customerProjects = projects.findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByStartDateDesc(companyId, id);
        var customerInvoices = invoices.findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByDueDateAsc(companyId, id);
        List<BusinessDocument> customerDocuments = documents.findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByCreatedAtDesc(
                companyId, id);
        model.addAttribute("pageTitle", "Cliente");
        model.addAttribute("customer", customerService.getCurrentCompanyCustomer(id));
        model.addAttribute("summary", CustomerBusinessSummary.from(customerProjects, customerInvoices));
        model.addAttribute("projects", customerProjects);
        model.addAttribute("quotations", quotations.findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByIssueDateDesc(
                companyId, id));
        model.addAttribute("invoices", customerInvoices);
        model.addAttribute("documents", customerDocuments);
        model.addAttribute("fileAvailability", fileAvailability(customerDocuments));
        return "customers/detail";
    }

    private Map<Long, Boolean> fileAvailability(List<BusinessDocument> relatedDocuments) {
        Map<Long, Boolean> availability = new LinkedHashMap<>();
        for (BusinessDocument document : relatedDocuments) {
            availability.put(document.getId(), documentService.isFileAvailable(document));
        }
        return availability;
    }

    @GetMapping("/customers/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        Customer customer = customerService.getCurrentCompanyCustomer(id);
        model.addAttribute("pageTitle", "Editar cliente");
        model.addAttribute("customer", customer);
        model.addAttribute("customerForm", CustomerForm.from(customer));
        return "customers/edit";
    }

    @PostMapping("/customers/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("customerForm") CustomerForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar cliente");
            model.addAttribute("customer", customerService.getCurrentCompanyCustomer(id));
            return "customers/edit";
        }
        Customer customer = customerService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Clientes", id,
                "Cliente actualizado: " + customer.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Cliente actualizado correctamente.");
        return "redirect:/customers/{id}";
    }

    @PostMapping("/customers/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Customer customer = customerService.getCurrentCompanyCustomer(id);
        customerService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Clientes", id,
                "Cliente archivado: " + customer.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Cliente archivado correctamente.");
        return "redirect:/customers";
    }

    private void addIndexData(Model model, String query, int page) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("name").ascending());
        model.addAttribute("pageTitle", "Clientes");
        model.addAttribute("customerPage", cleanQuery == null
                ? customers.findByCompanyAccountIdAndActiveTrue(companyId, pageable)
                : customers.searchByCompanyId(companyId, cleanQuery, pageable));
        model.addAttribute("query", cleanQuery);
    }
}
