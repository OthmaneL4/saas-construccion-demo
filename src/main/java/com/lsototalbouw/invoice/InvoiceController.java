package com.lsototalbouw.invoice;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.document.BusinessDocument;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.document.DocumentService;
import com.lsototalbouw.payment.PaymentRepository;
import com.lsototalbouw.pdf.BusinessPdfService;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class InvoiceController {

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final InvoiceService invoiceService;
    private final InvoiceLineService invoiceLineService;
    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;
    private final BusinessPdfService pdfService;
    private final AuditService auditService;
    private final InvoicePaymentReminderService paymentReminderService;
    private final BusinessDocumentRepository documents;
    private final DocumentService documentService;
    private final PaymentRepository payments;

    public InvoiceController(InvoiceRepository invoices, InvoiceLineRepository invoiceLines,
                             InvoiceService invoiceService, InvoiceLineService invoiceLineService,
                               CustomerRepository customers, ProjectRepository projects,
                               CompanyContextService companyContext, BusinessPdfService pdfService,
                               AuditService auditService, InvoicePaymentReminderService paymentReminderService,
                               BusinessDocumentRepository documents, DocumentService documentService,
                               PaymentRepository payments) {
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.invoiceService = invoiceService;
        this.invoiceLineService = invoiceLineService;
        this.customers = customers;
        this.projects = projects;
        this.companyContext = companyContext;
        this.pdfService = pdfService;
        this.auditService = auditService;
        this.paymentReminderService = paymentReminderService;
        this.documents = documents;
        this.documentService = documentService;
        this.payments = payments;
    }

    @GetMapping("/invoices")
    public String index(Model model, @ModelAttribute("invoiceForm") InvoiceForm invoiceForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "status", required = false) InvoiceStatus status,
                        @RequestParam(name = "dueFrom", required = false) LocalDate dueFrom,
                        @RequestParam(name = "dueTo", required = false) LocalDate dueTo,
                        @RequestParam(name = "page", defaultValue = "0") int page) {
        Long companyId = companyContext.currentCompanyId();
        addIndexData(model, query, status, dueFrom, dueTo, page);
        model.addAttribute("invoiceForm", preparedInvoiceForm(invoiceForm));
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("paymentTermsDays", invoiceService.currentCompanyPaymentTermsDays());
        return "invoices/index";
    }

    @PostMapping("/invoices")
    public String create(@Valid @ModelAttribute("invoiceForm") InvoiceForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        invoiceService.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            Long companyId = companyContext.currentCompanyId();
            addIndexData(model, null, null, null, null, 0);
            model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
            model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
            model.addAttribute("paymentTermsDays", invoiceService.currentCompanyPaymentTermsDays());
            return "invoices/index";
        }
        Invoice invoice = invoiceService.create(form);
        auditService.record(AuditAction.CREATE, "Facturas", invoice.getId(),
                "Factura creada: " + invoice.getInvoiceNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Factura creada correctamente.");
        return "redirect:/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        List<BusinessDocument> invoiceDocuments = documents.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtDesc(
                companyId, id);
        model.addAttribute("pageTitle", "Factura");
        model.addAttribute("invoice", invoiceService.getCurrentCompanyInvoice(id));
        model.addAttribute("invoiceLines", invoiceLines.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(companyId, id));
        model.addAttribute("paymentReminders", paymentReminderService.listForInvoice(id));
        model.addAttribute("payments", payments.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(
                companyId, id));
        model.addAttribute("reminderTemplates", InvoicePaymentReminderTemplate.values());
        model.addAttribute("documents", invoiceDocuments);
        model.addAttribute("fileAvailability", fileAvailability(invoiceDocuments));
        model.addAttribute("invoiceLineForm", new InvoiceLineForm());
        return "invoices/detail";
    }

    private Map<Long, Boolean> fileAvailability(List<BusinessDocument> relatedDocuments) {
        Map<Long, Boolean> availability = new LinkedHashMap<>();
        for (BusinessDocument document : relatedDocuments) {
            availability.put(document.getId(), documentService.isFileAvailable(document));
        }
        return availability;
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") Long id) {
        Invoice invoice = invoiceService.getCurrentCompanyInvoice(id);
        byte[] pdf = pdfService.invoicePdf(invoice,
                invoiceLines.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(
                        companyContext.currentCompanyId(), id));
        auditService.record(AuditAction.PDF_GENERATE, "Facturas", id,
                "PDF de factura generado: " + invoice.getInvoiceNumber());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("factura-" + invoice.getInvoiceNumber() + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdf);
    }

    @PostMapping("/invoices/{id}/lines")
    public String createLine(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("invoiceLineForm") InvoiceLineForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Factura");
            model.addAttribute("invoice", invoiceService.getCurrentCompanyInvoice(id));
            model.addAttribute("invoiceLines", invoiceLines.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(companyContext.currentCompanyId(), id));
            model.addAttribute("paymentReminders", paymentReminderService.listForInvoice(id));
            model.addAttribute("payments", payments.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(
                    companyContext.currentCompanyId(), id));
            model.addAttribute("reminderTemplates", InvoicePaymentReminderTemplate.values());
            model.addAttribute("documents", documents.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtDesc(
                    companyContext.currentCompanyId(), id));
            return "invoices/detail";
        }
        invoiceLineService.create(id, form);
        auditService.record(AuditAction.CREATE, "Facturas", id,
                "Linea de factura anadida", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Linea de factura anadida correctamente.");
        return "redirect:/invoices/{id}";
    }

    @GetMapping("/invoices/{id}/lines/{lineId}/edit")
    public String editLine(@PathVariable("id") Long id, @PathVariable("lineId") Long lineId, Model model) {
        InvoiceLine line = invoiceLineService.getCurrentCompanyInvoiceLine(id, lineId);
        model.addAttribute("pageTitle", "Editar linea");
        model.addAttribute("invoice", invoiceService.getCurrentCompanyInvoice(id));
        model.addAttribute("line", line);
        model.addAttribute("invoiceLineForm", InvoiceLineForm.from(line));
        return "invoices/line-edit";
    }

    @PostMapping("/invoices/{id}/lines/{lineId}/edit")
    public String updateLine(@PathVariable("id") Long id,
                             @PathVariable("lineId") Long lineId,
                             @Valid @ModelAttribute("invoiceLineForm") InvoiceLineForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar linea");
            model.addAttribute("invoice", invoiceService.getCurrentCompanyInvoice(id));
            model.addAttribute("line", invoiceLineService.getCurrentCompanyInvoiceLine(id, lineId));
            return "invoices/line-edit";
        }
        invoiceLineService.update(id, lineId, form);
        auditService.record(AuditAction.UPDATE, "Facturas", id,
                "Linea de factura actualizada", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Linea de factura actualizada correctamente.");
        return "redirect:/invoices/{id}";
    }

    @PostMapping("/invoices/{id}/lines/{lineId}/archive")
    public String archiveLine(@PathVariable("id") Long id, @PathVariable("lineId") Long lineId,
                              RedirectAttributes redirectAttributes) {
        invoiceLineService.archive(id, lineId);
        auditService.record(AuditAction.ARCHIVE, "Facturas", id,
                "Linea de factura archivada", "Linea " + lineId);
        redirectAttributes.addFlashAttribute("successMessage", "Linea de factura archivada correctamente.");
        return "redirect:/invoices/{id}";
    }

    @GetMapping("/invoices/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        Invoice invoice = invoiceService.getCurrentCompanyInvoice(id);
        model.addAttribute("pageTitle", "Editar factura");
        model.addAttribute("invoice", invoice);
        model.addAttribute("invoiceForm", InvoiceForm.from(invoice));
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("statuses", InvoiceStatus.values());
        return "invoices/edit";
    }

    @PostMapping("/invoices/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("invoiceForm") InvoiceForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long companyId = companyContext.currentCompanyId();
        invoiceService.validate(form, id, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar factura");
            model.addAttribute("invoice", invoiceService.getCurrentCompanyInvoice(id));
            model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
            model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
            model.addAttribute("statuses", InvoiceStatus.values());
            return "invoices/edit";
        }
        invoiceService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Facturas", id,
                "Factura actualizada: " + form.getInvoiceNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Factura actualizada correctamente.");
        return "redirect:/invoices/{id}";
    }

    @PostMapping("/invoices/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        invoiceService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Facturas", id, "Factura archivada");
        redirectAttributes.addFlashAttribute("successMessage", "Factura archivada correctamente.");
        return "redirect:/invoices";
    }

    @PostMapping("/invoices/{id}/payment-reminder")
    public String markPaymentReminderSent(@PathVariable("id") Long id,
                                           @RequestParam(name = "template", required = false) String template,
                                           @RequestParam(name = "message", required = false) String message,
                                           RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.markPaymentReminderSent(id);
            paymentReminderService.record(invoice, InvoicePaymentReminderType.MANUAL,
                    InvoicePaymentReminderStatus.REGISTERED,
                    reminderMessage(template, message, "Recordatorio de pago registrado manualmente"));
            auditService.record(AuditAction.REMINDER_SENT, "Facturas", id,
                    "Recordatorio de pago registrado", invoice.getInvoiceNumber());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Recordatorio de pago registrado para " + invoice.getInvoiceNumber() + ".");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/invoices/{id}";
    }

    @PostMapping("/invoices/{id}/payment-reminder/pdf")
    public Object paymentReminderPdf(@PathVariable("id") Long id,
                                      @RequestParam(name = "template", required = false) String template,
                                      @RequestParam(name = "message", required = false) String message,
                                      RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.markPaymentReminderSent(id);
            String cleanMessage = reminderMessage(template, message, "Carta PDF de reclamacion generada");
            paymentReminderService.record(invoice, InvoicePaymentReminderType.PDF,
                    InvoicePaymentReminderStatus.GENERATED,
                    cleanMessage);
            auditService.record(AuditAction.REMINDER_SENT, "Facturas", id,
                    "Reclamacion de pago generada", invoice.getInvoiceNumber());
            auditService.record(AuditAction.PDF_GENERATE, "Facturas", id,
                    "PDF de reclamacion generado: " + invoice.getInvoiceNumber());
            byte[] pdf = pdfService.paymentReminderPdf(invoice, cleanMessage);
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename("reclamacion-" + invoice.getInvoiceNumber() + ".pdf")
                    .build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(pdf);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices/{id}";
        }
    }

    private void addIndexData(Model model, String query, InvoiceStatus status,
                              LocalDate dueFrom, LocalDate dueTo, int page) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("dueDate").ascending());
        boolean hasFilters = cleanQuery != null || status != null || dueFrom != null || dueTo != null;
        List<Invoice> filteredInvoices = hasFilters
                ? invoices.searchByCompanyId(companyId, cleanQuery, status, dueFrom, dueTo)
                : invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyId);
        BigDecimal totalInvoiced = filteredInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = filteredInvoices.stream()
                .map(Invoice::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("pageTitle", "Facturas");
        model.addAttribute("invoicePage", !hasFilters
                ? invoices.findPageByCompanyId(companyId, pageable)
                : invoices.searchPageByCompanyId(companyId, cleanQuery, status, dueFrom, dueTo, pageable));
        model.addAttribute("invoiceCount", filteredInvoices.size());
        model.addAttribute("totalFilteredInvoiced", totalInvoiced);
        model.addAttribute("totalFilteredPaid", totalPaid);
        model.addAttribute("totalFilteredOutstanding", totalInvoiced.subtract(totalPaid));
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("dueFrom", dueFrom);
        model.addAttribute("dueTo", dueTo);
        model.addAttribute("statuses", InvoiceStatus.values());
    }

    private String reminderMessage(String template, String message, String fallback) {
        String templateMessage = InvoicePaymentReminderTemplate.fromCode(template)
                .map(InvoicePaymentReminderTemplate::getMessage)
                .orElse(null);
        String customMessage = message == null || message.isBlank() ? null : message.trim();
        String combined;
        if (templateMessage != null && customMessage != null) {
            combined = templateMessage + System.lineSeparator() + System.lineSeparator() + "Nota: " + customMessage;
        } else if (templateMessage != null) {
            combined = templateMessage;
        } else if (customMessage != null) {
            combined = customMessage;
        } else {
            return fallback;
        }
        return combined.length() <= 1000 ? combined : combined.substring(0, 1000);
    }

    private InvoiceForm preparedInvoiceForm(InvoiceForm incomingForm) {
        if (incomingForm == null || incomingForm.isEmptyNewForm()) {
            return invoiceService.newInvoiceForm();
        }

        InvoiceForm form = invoiceService.newInvoiceForm();
        form.setCustomerId(incomingForm.getCustomerId());
        form.setProjectId(incomingForm.getProjectId());
        form.setAmount(incomingForm.getAmount());
        form.setPaidAmount(incomingForm.getPaidAmount());
        form.setStatus(incomingForm.getStatus());

        if (incomingForm.getProjectId() != null) {
            projects.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), incomingForm.getProjectId())
                    .map(Project::getCustomer)
                    .ifPresent(customer -> form.setCustomerId(customer.getId()));
        }
        return form;
    }
}
