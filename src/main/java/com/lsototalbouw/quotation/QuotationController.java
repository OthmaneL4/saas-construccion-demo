package com.lsototalbouw.quotation;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.enums.QuotationStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.pdf.BusinessPdfService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
public class QuotationController {

    private final QuotationRepository quotations;
    private final QuotationLineRepository quotationLines;
    private final QuotationService quotationService;
    private final QuotationLineService quotationLineService;
    private final CustomerRepository customers;
    private final CompanyContextService companyContext;
    private final BusinessPdfService pdfService;
    private final AuditService auditService;
    private final QuotationInvoiceConversionService conversionService;

    public QuotationController(QuotationRepository quotations, QuotationLineRepository quotationLines,
                               QuotationService quotationService, QuotationLineService quotationLineService,
                               CustomerRepository customers, CompanyContextService companyContext,
                               BusinessPdfService pdfService, AuditService auditService,
                               QuotationInvoiceConversionService conversionService) {
        this.quotations = quotations;
        this.quotationLines = quotationLines;
        this.quotationService = quotationService;
        this.quotationLineService = quotationLineService;
        this.customers = customers;
        this.companyContext = companyContext;
        this.pdfService = pdfService;
        this.auditService = auditService;
        this.conversionService = conversionService;
    }

    @GetMapping("/quotations")
    public String index(Model model, @ModelAttribute("quotationForm") QuotationForm quotationForm,
                        @RequestParam(name = "customerId", required = false) Long customerId,
                        @RequestParam(name = "status", required = false) QuotationStatus status,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "validFrom", required = false) LocalDate validFrom,
                        @RequestParam(name = "validTo", required = false) LocalDate validTo) {
        prepareModel(model, customerId, status, query, validFrom, validTo);
        model.addAttribute("quotationForm", preparedQuotationForm(quotationForm));
        return "quotations/index";
    }

    @PostMapping("/quotations")
    public String create(@Valid @ModelAttribute("quotationForm") QuotationForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        quotationService.validate(form, null, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareModel(model, form.getCustomerId(), null, null, null, null);
            return "quotations/index";
        }
        Quotation quotation = quotationService.create(form);
        auditService.record(AuditAction.CREATE, "Presupuestos", quotation.getId(),
                "Presupuesto creado: " + quotation.getQuotationNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Presupuesto creado correctamente.");
        return "redirect:/quotations";
    }

    @GetMapping("/quotations/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Quotation quotation = quotationService.getCurrentCompanyQuotation(id);
        Invoice convertedInvoice = conversionService.convertedInvoice(id).orElse(null);
        model.addAttribute("pageTitle", "Presupuesto");
        model.addAttribute("quotation", quotation);
        model.addAttribute("convertedInvoice", convertedInvoice);
        model.addAttribute("quotationLines", quotationLines.findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(companyContext.currentCompanyId(), id));
        model.addAttribute("quotationLineForm", new QuotationLineForm());
        return "quotations/detail";
    }

    @GetMapping("/quotations/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") Long id) {
        Quotation quotation = quotationService.getCurrentCompanyQuotation(id);
        byte[] pdf = pdfService.quotationPdf(quotation,
                quotationLines.findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(
                        companyContext.currentCompanyId(), id));
        auditService.record(AuditAction.PDF_GENERATE, "Presupuestos", id,
                "PDF de presupuesto generado: " + quotation.getQuotationNumber());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("presupuesto-" + quotation.getQuotationNumber() + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdf);
    }

    @PostMapping("/quotations/{id}/lines")
    public String createLine(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("quotationLineForm") QuotationLineForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Presupuesto");
            model.addAttribute("quotation", quotationService.getCurrentCompanyQuotation(id));
            model.addAttribute("quotationLines", quotationLines.findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(companyContext.currentCompanyId(), id));
            return "quotations/detail";
        }
        quotationLineService.create(id, form);
        auditService.record(AuditAction.CREATE, "Presupuestos", id,
                "Linea de presupuesto anadida", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Linea de presupuesto anadida correctamente.");
        return "redirect:/quotations/{id}";
    }

    @GetMapping("/quotations/{id}/lines/{lineId}/edit")
    public String editLine(@PathVariable("id") Long id, @PathVariable("lineId") Long lineId, Model model) {
        QuotationLine line = quotationLineService.getCurrentCompanyQuotationLine(id, lineId);
        model.addAttribute("pageTitle", "Editar linea");
        model.addAttribute("quotation", quotationService.getCurrentCompanyQuotation(id));
        model.addAttribute("line", line);
        model.addAttribute("quotationLineForm", QuotationLineForm.from(line));
        return "quotations/line-edit";
    }

    @PostMapping("/quotations/{id}/lines/{lineId}/edit")
    public String updateLine(@PathVariable("id") Long id,
                             @PathVariable("lineId") Long lineId,
                             @Valid @ModelAttribute("quotationLineForm") QuotationLineForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar linea");
            model.addAttribute("quotation", quotationService.getCurrentCompanyQuotation(id));
            model.addAttribute("line", quotationLineService.getCurrentCompanyQuotationLine(id, lineId));
            return "quotations/line-edit";
        }
        quotationLineService.update(id, lineId, form);
        auditService.record(AuditAction.UPDATE, "Presupuestos", id,
                "Linea de presupuesto actualizada", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Linea de presupuesto actualizada correctamente.");
        return "redirect:/quotations/{id}";
    }

    @PostMapping("/quotations/{id}/lines/{lineId}/archive")
    public String archiveLine(@PathVariable("id") Long id, @PathVariable("lineId") Long lineId,
                              RedirectAttributes redirectAttributes) {
        quotationLineService.archive(id, lineId);
        auditService.record(AuditAction.ARCHIVE, "Presupuestos", id,
                "Linea de presupuesto archivada", "Linea " + lineId);
        redirectAttributes.addFlashAttribute("successMessage", "Linea de presupuesto archivada correctamente.");
        return "redirect:/quotations/{id}";
    }

    @GetMapping("/quotations/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        Quotation quotation = quotationService.getCurrentCompanyQuotation(id);
        prepareModel(model, null);
        model.addAttribute("pageTitle", "Editar presupuesto");
        model.addAttribute("quotation", quotation);
        model.addAttribute("quotationForm", QuotationForm.from(quotation));
        return "quotations/edit";
    }

    @PostMapping("/quotations/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("quotationForm") QuotationForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        quotationService.validate(form, id, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareModel(model, form.getCustomerId());
            model.addAttribute("pageTitle", "Editar presupuesto");
            model.addAttribute("quotation", quotationService.getCurrentCompanyQuotation(id));
            return "quotations/edit";
        }
        quotationService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Presupuestos", id,
                "Presupuesto actualizado: " + form.getQuotationNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Presupuesto actualizado correctamente.");
        return "redirect:/quotations/{id}";
    }

    @PostMapping("/quotations/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        quotationService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Presupuestos", id, "Presupuesto archivado");
        redirectAttributes.addFlashAttribute("successMessage", "Presupuesto archivado correctamente.");
        return "redirect:/quotations";
    }

    @PostMapping("/quotations/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") QuotationStatus status,
                               RedirectAttributes redirectAttributes) {
        Quotation quotation = quotationService.updateStatus(id, status);
        auditService.record(AuditAction.UPDATE, "Presupuestos", id,
                "Estado de presupuesto actualizado", quotation.getQuotationNumber() + " -> " + status);
        redirectAttributes.addFlashAttribute("successMessage", "Estado del presupuesto actualizado correctamente.");
        return "redirect:/quotations/{id}";
    }

    @PostMapping("/quotations/{id}/convert-to-invoice")
    public String convertToInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = conversionService.convertToInvoice(id);
            auditService.record(AuditAction.CONVERT, "Presupuestos", id,
                    "Presupuesto convertido en factura", invoice.getInvoiceNumber());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Presupuesto convertido en factura " + invoice.getInvoiceNumber() + ".");
            return "redirect:/invoices/" + invoice.getId();
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/quotations/{id}";
        }
    }

    private void prepareModel(Model model, Long customerId) {
        prepareModel(model, customerId, null, null, null, null);
    }

    private void prepareModel(Model model, Long customerId, QuotationStatus status, String query,
                              LocalDate validFrom, LocalDate validTo) {
        Long companyId = companyContext.currentCompanyId();
        Long selectedCustomerId = customerId == null ? null
                : customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, customerId)
                .map(customer -> customerId)
                .orElse(null);
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        boolean hasFilters = selectedCustomerId != null || status != null || cleanQuery != null
                || validFrom != null || validTo != null;
        List<Quotation> filteredQuotations = hasFilters
                ? quotations.searchByCompanyId(companyId, selectedCustomerId, status, cleanQuery, validFrom, validTo)
                : quotations.findByCompanyAccountIdAndActiveTrueOrderByIssueDateDesc(companyId);
        BigDecimal totalQuoted = filteredQuotations.stream()
                .map(Quotation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal acceptedQuoted = filteredQuotations.stream()
                .filter(quotation -> quotation.getStatus() == QuotationStatus.ACCEPTED)
                .map(Quotation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("pageTitle", "Presupuestos");
        model.addAttribute("quotations", filteredQuotations);
        model.addAttribute("quotationCount", filteredQuotations.size());
        model.addAttribute("totalQuoted", totalQuoted);
        model.addAttribute("acceptedQuoted", acceptedQuoted);
        model.addAttribute("acceptedCount", filteredQuotations.stream()
                .filter(quotation -> quotation.getStatus() == QuotationStatus.ACCEPTED)
                .count());
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        model.addAttribute("statuses", QuotationStatus.values());
        model.addAttribute("selectedCustomerId", selectedCustomerId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("query", cleanQuery);
        model.addAttribute("validFrom", validFrom);
        model.addAttribute("validTo", validTo);
    }

    private QuotationForm preparedQuotationForm(QuotationForm incomingForm) {
        if (incomingForm == null || incomingForm.isEmptyNewForm()) {
            return quotationService.newQuotationForm();
        }

        QuotationForm form = quotationService.newQuotationForm();
        form.setCustomerId(incomingForm.getCustomerId());
        form.setTitle(incomingForm.getTitle());
        form.setDescription(incomingForm.getDescription());
        form.setAmount(incomingForm.getAmount());
        form.setStatus(incomingForm.getStatus());
        return form;
    }
}
