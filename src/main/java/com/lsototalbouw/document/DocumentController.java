package com.lsototalbouw.document;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.web.SafeRedirects;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.notification.BusinessNotification;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.project.ProjectRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
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
public class DocumentController {

    private final BusinessDocumentRepository documents;
    private final DocumentService documentService;
    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;
    private final AuditService auditService;
    private final BusinessNotificationRepository notifications;

    public DocumentController(BusinessDocumentRepository documents, DocumentService documentService,
                              CustomerRepository customers, ProjectRepository projects,
                              InvoiceRepository invoices, CompanyContextService companyContext,
                              AuditService auditService, BusinessNotificationRepository notifications) {
        this.documents = documents;
        this.documentService = documentService;
        this.customers = customers;
        this.projects = projects;
        this.invoices = invoices;
        this.companyContext = companyContext;
        this.auditService = auditService;
        this.notifications = notifications;
    }

    @GetMapping("/documents")
    public String index(Model model, @ModelAttribute("documentForm") DocumentForm documentForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "category", required = false) DocumentCategory category,
                        @RequestParam(name = "customerId", required = false) Long customerId,
                        @RequestParam(name = "projectId", required = false) Long projectId,
                        @RequestParam(name = "invoiceId", required = false) Long invoiceId,
                        @RequestParam(name = "unavailableOnly", defaultValue = "false") boolean unavailableOnly,
                        @RequestParam(name = "returnUrl", required = false) String returnUrl) {
        addIndexData(model, query, category, customerId, projectId, invoiceId, unavailableOnly);
        applyPrefill(documentForm, customerId, projectId, invoiceId, returnUrl);
        model.addAttribute("documentForm", documentForm);
        return "documents/index";
    }

    @PostMapping("/documents")
    public String create(@Valid @ModelAttribute("documentForm") DocumentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        documentService.validateUpload(form, bindingResult);
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, null, form.getCustomerId(), form.getProjectId(), form.getInvoiceId(), false);
            return "documents/index";
        }
        BusinessDocument document = documentService.create(form);
        auditService.record(AuditAction.CREATE, "Documentos", document.getId(),
                "Documento subido: " + document.getTitle(), document.getOriginalFilename());
        redirectAttributes.addFlashAttribute("successMessage", "Documento subido correctamente.");
        return SafeRedirects.redirectTo(form.getReturnUrl(), "/documents");
    }

    @GetMapping("/documents/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        BusinessDocument document = documentService.getCurrentCompanyDocument(id);
        model.addAttribute("pageTitle", "Documento");
        model.addAttribute("document", document);
        model.addAttribute("fileAvailable", documentService.isFileAvailable(document));
        return "documents/detail";
    }

    @GetMapping("/documents/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        BusinessDocument document = documentService.getCurrentCompanyDocument(id);
        model.addAttribute("pageTitle", "Editar documento");
        model.addAttribute("document", document);
        model.addAttribute("documentForm", DocumentForm.from(document));
        addReferenceData(model);
        return "documents/edit";
    }

    @PostMapping("/documents/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("documentForm") DocumentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar documento");
            model.addAttribute("document", documentService.getCurrentCompanyDocument(id));
            addReferenceData(model);
            return "documents/edit";
        }
        documentService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Documentos", id, "Documento actualizado");
        redirectAttributes.addFlashAttribute("successMessage", "Documento actualizado correctamente.");
        return "redirect:/documents/{id}";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        BusinessDocument document = documentService.getCurrentCompanyDocument(id);
        Resource resource = loadVerifiedResource(document);
        auditService.record(AuditAction.DOWNLOAD, "Documentos", id,
                "Documento descargado: " + document.getTitle(), document.getOriginalFilename());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(document.getOriginalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    @GetMapping("/documents/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable("id") Long id) {
        BusinessDocument document = documentService.getCurrentCompanyDocument(id);
        Resource resource = loadVerifiedResource(document);
        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(document.getOriginalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    private Resource loadVerifiedResource(BusinessDocument document) {
        try {
            return documentService.loadAsResource(document);
        } catch (IllegalArgumentException ex) {
            if (isIntegrityFailure(ex)) {
                auditService.record(AuditAction.DOCUMENT_INTEGRITY_FAILURE, "Documentos", document.getId(),
                        "Fallo de integridad en documento: " + document.getTitle(),
                        document.getOriginalFilename());
                notifyIntegrityFailure(document);
            }
            throw ex;
        }
    }

    private void notifyIntegrityFailure(BusinessDocument document) {
        String sourceKey = "document-integrity:" + document.getId();
        String title = "Documento con integridad comprometida";
        String message = "El archivo " + document.getOriginalFilename()
                + " no coincide con su huella SHA-256. Revisa la copia almacenada antes de usarlo.";
        String targetUrl = "/documents/" + document.getId();
        notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(
                        document.getCompanyAccount().getId(), sourceKey)
                .ifPresentOrElse(existing -> existing.updateFrom(
                                "danger", title, message, targetUrl, LocalDate.now()),
                        () -> notifications.save(new BusinessNotification(
                                document.getCompanyAccount(), sourceKey, "danger", title,
                                message, targetUrl, LocalDate.now())));
    }

    private boolean isIntegrityFailure(IllegalArgumentException ex) {
        return ex.getMessage() != null && ex.getMessage().toLowerCase().contains("integridad");
    }

    @PostMapping("/documents/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        documentService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Documentos", id, "Documento archivado");
        redirectAttributes.addFlashAttribute("successMessage", "Documento archivado correctamente.");
        return "redirect:/documents";
    }

    private void addIndexData(Model model, String query, DocumentCategory category,
                              Long customerId, Long projectId, Long invoiceId, boolean unavailableOnly) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        List<BusinessDocument> filteredDocuments = cleanQuery == null && category == null && customerId == null
                && projectId == null && invoiceId == null
                ? documents.findByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(companyId)
                : documents.searchByCompanyId(companyId, cleanQuery, category, customerId, projectId, invoiceId);
        Map<Long, Boolean> fileAvailability = fileAvailability(filteredDocuments);
        if (unavailableOnly) {
            Map<Long, Boolean> initialFileAvailability = fileAvailability;
            filteredDocuments = filteredDocuments.stream()
                    .filter(document -> !Boolean.TRUE.equals(initialFileAvailability.get(document.getId())))
                    .toList();
            fileAvailability = fileAvailability(filteredDocuments);
        }
        model.addAttribute("pageTitle", "Documentos");
        model.addAttribute("documents", filteredDocuments);
        model.addAttribute("fileAvailability", fileAvailability);
        model.addAttribute("summary", DocumentLibrarySummary.from(filteredDocuments, fileAvailability));
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("selectedProjectId", projectId);
        model.addAttribute("selectedInvoiceId", invoiceId);
        model.addAttribute("unavailableOnly", unavailableOnly);
        addReferenceData(model);
    }

    private Map<Long, Boolean> fileAvailability(List<BusinessDocument> filteredDocuments) {
        Map<Long, Boolean> availability = new LinkedHashMap<>();
        for (BusinessDocument document : filteredDocuments) {
            availability.put(document.getId(), documentService.isFileAvailable(document));
        }
        return availability;
    }

    private void addReferenceData(Model model) {
        Long companyId = companyContext.currentCompanyId();
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("invoices", invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyId));
    }

    private void applyPrefill(DocumentForm documentForm, Long customerId, Long projectId,
                              Long invoiceId, String returnUrl) {
        if (documentForm.getCustomerId() == null) {
            documentForm.setCustomerId(customerId);
        }
        if (documentForm.getProjectId() == null) {
            documentForm.setProjectId(projectId);
        }
        if (documentForm.getInvoiceId() == null) {
            documentForm.setInvoiceId(invoiceId);
        }
        documentForm.setReturnUrl(safeReturnUrl(documentForm.getReturnUrl(), returnUrl));
    }

    private String safeReturnUrl(String formReturnUrl, String requestReturnUrl) {
        if (SafeRedirects.isLocalPath(formReturnUrl)) {
            return formReturnUrl;
        }
        if (SafeRedirects.isLocalPath(requestReturnUrl)) {
            return requestReturnUrl;
        }
        return null;
    }
}
