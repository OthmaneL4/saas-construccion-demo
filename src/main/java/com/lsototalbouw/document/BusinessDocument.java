package com.lsototalbouw.document;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_documents", indexes = {
        @Index(name = "idx_documents_company_active_category", columnList = "company_account_id, active, category"),
        @Index(name = "idx_documents_customer", columnList = "customer_id"),
        @Index(name = "idx_documents_project", columnList = "project_id")
})
public class BusinessDocument extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, length = 180)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentCategory category = DocumentCategory.OTHER;

    @Column(nullable = false, length = 240)
    private String originalFilename;

    @Column(nullable = false, unique = true, length = 120)
    private String storedFilename;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @Column(length = 1000)
    private String notes;

    protected BusinessDocument() {
    }

    public BusinessDocument(CompanyAccount companyAccount, Customer customer, Project project, String title,
                            DocumentCategory category, String originalFilename, String storedFilename,
                            String contentType, long fileSize, String notes) {
        this(companyAccount, customer, project, null, title, category, originalFilename, storedFilename,
                contentType, fileSize, notes);
    }

    public BusinessDocument(CompanyAccount companyAccount, Customer customer, Project project, Invoice invoice,
                            String title, DocumentCategory category, String originalFilename, String storedFilename,
                            String contentType, long fileSize, String notes) {
        this(companyAccount, customer, project, invoice, title, category, originalFilename, storedFilename,
                contentType, fileSize, null, notes);
    }

    public BusinessDocument(CompanyAccount companyAccount, Customer customer, Project project, Invoice invoice,
                            String title, DocumentCategory category, String originalFilename, String storedFilename,
                            String contentType, long fileSize, String sha256Checksum, String notes) {
        this.companyAccount = companyAccount;
        this.customer = customer;
        this.project = project;
        this.invoice = invoice;
        this.title = title;
        this.category = category;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sha256Checksum = sha256Checksum;
        this.notes = notes;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Project getProject() {
        return project;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getTitle() {
        return title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public String getShortSha256Checksum() {
        if (sha256Checksum == null || sha256Checksum.length() <= 16) {
            return sha256Checksum;
        }
        return sha256Checksum.substring(0, 16) + "...";
    }

    public String getNotes() {
        return notes;
    }

    public String getReadableFileSize() {
        if (fileSize >= 1024 * 1024) {
            return String.format("%.1f MB", fileSize / 1024.0 / 1024.0);
        }
        if (fileSize >= 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        }
        return fileSize + " B";
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    public String getFileKindLabel() {
        if (isPdf()) {
            return "PDF";
        }
        if (isImage()) {
            return "Imagen";
        }
        return "Archivo";
    }

    public void updateMetadata(Customer customer, Project project, Invoice invoice, String title,
                               DocumentCategory category, String notes) {
        this.customer = customer;
        this.project = project;
        this.invoice = invoice;
        this.title = title;
        this.category = category;
        this.notes = notes;
    }

    public void updateMetadata(Customer customer, Project project, String title, DocumentCategory category, String notes) {
        updateMetadata(customer, project, null, title, category, notes);
    }
}
