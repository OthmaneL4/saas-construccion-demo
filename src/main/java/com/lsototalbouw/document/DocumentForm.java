package com.lsototalbouw.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class DocumentForm {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 180)
    private String title;

    @NotNull(message = "Selecciona una categoria")
    private DocumentCategory category = DocumentCategory.OTHER;

    private Long customerId;

    private Long projectId;

    private Long invoiceId;

    @Size(max = 1000)
    private String notes;

    @Size(max = 240)
    private String returnUrl;

    private MultipartFile file;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public static DocumentForm from(BusinessDocument document) {
        DocumentForm form = new DocumentForm();
        form.setTitle(document.getTitle());
        form.setCategory(document.getCategory());
        form.setCustomerId(document.getCustomer() == null ? null : document.getCustomer().getId());
        form.setProjectId(document.getProject() == null ? null : document.getProject().getId());
        form.setInvoiceId(document.getInvoice() == null ? null : document.getInvoice().getId());
        form.setNotes(document.getNotes());
        return form;
    }
}
