package com.lsototalbouw.document;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles secure document metadata management and file storage.
 *
 * <p>The service validates uploaded files, stores them under generated server-side names, and keeps document
 * records scoped to the current company. It also enforces path normalization checks before storing or loading
 * files so user-controlled filenames cannot escape the configured document directory.
 */
@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "application/pdf", Set.of(".pdf"),
            "image/jpeg", Set.of(".jpg", ".jpeg"),
            "image/png", Set.of(".png"),
            "image/webp", Set.of(".webp")
    );

    private final BusinessDocumentRepository documents;
    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;
    private final DocumentSecurityScanner securityScanner;
    private final Path documentsDirectory;
    private final long maxDocumentSizeBytes;

    public DocumentService(BusinessDocumentRepository documents, CustomerRepository customers,
                            ProjectRepository projects, InvoiceRepository invoices, CompanyContextService companyContext,
                            DocumentSecurityScanner securityScanner,
                            @Value("${app.upload.documents-dir:uploads/documents}") String documentsDir,
                            @Value("${app.upload.max-document-size-bytes:10485760}") long maxDocumentSizeBytes) {
        this.documents = documents;
        this.customers = customers;
        this.projects = projects;
        this.invoices = invoices;
        this.companyContext = companyContext;
        this.securityScanner = securityScanner;
        this.documentsDirectory = Paths.get(documentsDir).toAbsolutePath().normalize();
        this.maxDocumentSizeBytes = maxDocumentSizeBytes;
    }

    /**
     * Stores a validated upload and creates the corresponding business document record.
     *
     * <p>When a document is linked to an invoice, the invoice's customer and project become the source of truth
     * for the association. This keeps document metadata aligned with the financial record.
     *
     * @param form validated document upload form
     * @return the persisted document metadata
     */
    @Transactional
    public BusinessDocument create(DocumentForm form) {
        CompanyAccount company = companyContext.currentCompany();
        Customer customer = findCustomer(company.getId(), form.getCustomerId());
        Project project = findProject(company.getId(), form.getProjectId());
        Invoice invoice = findInvoice(company.getId(), form.getInvoiceId());
        if (invoice != null) {
            customer = invoice.getCustomer();
            project = invoice.getProject() == null ? project : invoice.getProject();
        }
        MultipartFile file = form.getFile();
        String originalFilename = cleanFilename(file.getOriginalFilename());
        StoredDocument storedDocument = store(file, originalFilename);

        BusinessDocument document = new BusinessDocument(
                company,
                customer,
                project,
                invoice,
                form.getTitle().trim(),
                form.getCategory(),
                originalFilename,
                storedDocument.filename(),
                file.getContentType(),
                file.getSize(),
                storedDocument.sha256Checksum(),
                clean(form.getNotes())
        );
        return documents.save(document);
    }

    /**
     * Loads an active document belonging to the current company.
     *
     * @param id document identifier from the route
     * @return tenant-scoped document metadata
     * @throws IllegalArgumentException when the document is missing, archived, or belongs to another company
     */
    @Transactional(readOnly = true)
    public BusinessDocument getCurrentCompanyDocument(Long id) {
        return documents.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    /**
     * Updates document metadata without replacing the stored file.
     *
     * @param id   document identifier
     * @param form validated metadata form
     * @return the updated document metadata
     */
    @Transactional
    public BusinessDocument update(Long id, DocumentForm form) {
        Long companyId = companyContext.currentCompanyId();
        BusinessDocument document = documents.findByCompanyAccountIdAndIdAndActiveTrue(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        Invoice invoice = findInvoice(companyId, form.getInvoiceId());
        Customer customer = findCustomer(companyId, form.getCustomerId());
        Project project = findProject(companyId, form.getProjectId());
        if (invoice != null) {
            customer = invoice.getCustomer();
            project = invoice.getProject() == null ? project : invoice.getProject();
        }
        document.updateMetadata(
                customer,
                project,
                invoice,
                form.getTitle().trim(),
                form.getCategory(),
                clean(form.getNotes())
        );
        return document;
    }

    /**
     * Soft-archives a document while leaving the physical file available for retention policies.
     *
     * @param id document identifier
     */
    @Transactional
    public void archive(Long id) {
        BusinessDocument document = getCurrentCompanyDocument(id);
        document.setActive(false);
    }

    /**
     * Performs backend validation for document uploads.
     *
     * <p>The validation checks presence, size, MIME type, normalized filename, and extension-to-MIME alignment.
     * Controllers call this in addition to form validation because file properties cannot be trusted from the UI.
     *
     * @param form          submitted document form
     * @param bindingResult validation sink used by the MVC controller
     */
    public void validateUpload(DocumentForm form, BindingResult bindingResult) {
        MultipartFile file = form.getFile();
        if (file == null || file.isEmpty()) {
            bindingResult.rejectValue("file", "document.file.required", "Selecciona un archivo");
            return;
        }
        if (file.getSize() > maxDocumentSizeBytes) {
            bindingResult.rejectValue("file", "document.file.size",
                    "El archivo no puede superar " + readableSize(maxDocumentSizeBytes));
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            bindingResult.rejectValue("file", "document.file.type", "Solo se permiten PDF, JPG, PNG o WEBP");
        }
        String originalFilename = cleanFilename(file.getOriginalFilename());
        if (originalFilename.isBlank() || originalFilename.contains("..")) {
            bindingResult.rejectValue("file", "document.file.name", "Nombre de archivo no valido");
            return;
        }
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, Set.of()).contains(extension)) {
            bindingResult.rejectValue("file", "document.file.extension",
                    "La extension del archivo no coincide con el tipo permitido");
        }
    }

    /**
     * Resolves a stored document as a readable Spring resource.
     *
     * @param document document metadata containing the generated stored filename
     * @return a readable resource for download or preview
     * @throws IllegalArgumentException when the resolved file path is unsafe or unavailable
     */
    public Resource loadAsResource(BusinessDocument document) {
        try {
            Path filePath = documentsDirectory.resolve(document.getStoredFilename()).normalize();
            if (!filePath.startsWith(documentsDirectory)) {
                throw new IllegalArgumentException("Ruta de documento no permitida");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Archivo no disponible. Vuelve a subir el documento o revisa la carpeta de documentos.");
            }
            verifyChecksum(document, filePath);
            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Archivo no disponible. Vuelve a subir el documento o revisa la carpeta de documentos.", ex);
        }
    }

    /**
     * Checks whether the physical file for a document exists and can be read.
     *
     * @param document document metadata containing the generated stored filename
     * @return {@code true} when the file is inside the configured directory and readable
     */
    public boolean isFileAvailable(BusinessDocument document) {
        Path filePath = documentsDirectory.resolve(document.getStoredFilename()).normalize();
        return filePath.startsWith(documentsDirectory) && Files.isRegularFile(filePath) && Files.isReadable(filePath);
    }

    private StoredDocument store(MultipartFile file, String originalFilename) {
        try {
            Files.createDirectories(documentsDirectory);
            String extension = extensionOf(originalFilename);
            String storedFilename = UUID.randomUUID() + extension;
            Path target = documentsDirectory.resolve(storedFilename).normalize();
            if (!target.startsWith(documentsDirectory)) {
                throw new IllegalArgumentException("Ruta de documento no permitida");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            try {
                securityScanner.scan(target, originalFilename, normalizeContentType(file.getContentType()));
            } catch (RuntimeException ex) {
                Files.deleteIfExists(target);
                throw ex;
            }
            return new StoredDocument(storedFilename, sha256(target));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo", ex);
        }
    }

    private String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(file);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return toHex(digest.digest());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo verificar la integridad del archivo", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible en la JVM", ex);
        }
    }

    private void verifyChecksum(BusinessDocument document, Path filePath) {
        String expectedChecksum = document.getSha256Checksum();
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return;
        }
        String actualChecksum = sha256(filePath);
        if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            throw new IllegalArgumentException(
                    "El archivo no supera la verificacion de integridad. Revisa la copia almacenada antes de descargarlo.");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private Customer findCustomer(Long companyId, Long customerId) {
        if (customerId == null) {
            return null;
        }
        return customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    private Project findProject(Long companyId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    private Invoice findInvoice(Long companyId, Long invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyId, invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
    }

    private String cleanFilename(String filename) {
        return StringUtils.cleanPath(filename == null ? "" : filename).trim();
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
    }

    private String readableSize(long bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " MB";
        }
        if (bytes >= 1024) {
            return (bytes / 1024) + " KB";
        }
        return bytes + " B";
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record StoredDocument(String filename, String sha256Checksum) {
    }
}
