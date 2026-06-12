package com.lsototalbouw.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates production document storage before the application accepts traffic.
 *
 * <p>Uploaded documents are stored on disk, outside the database. In production, a missing,
 * non-directory, or non-writable document path would break customer/project/invoice document workflows.
 * This validator fails fast during startup so deployment issues are detected before users start working.
 */
@Component
@Profile("prod")
public class ProductionDocumentStorageValidator implements ApplicationRunner {

    private final String documentsDir;

    public ProductionDocumentStorageValidator(@Value("${app.upload.documents-dir:}") String documentsDir) {
        this.documentsDir = documentsDir;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    /**
     * Ensures the configured production document directory exists and supports basic write/delete operations.
     */
    public void validate() {
        if (!StringUtils.hasText(documentsDir)) {
            throw new IllegalStateException("Production requires DOCUMENTS_DIR/app.upload.documents-dir to be configured.");
        }
        Path directory = Paths.get(documentsDir).toAbsolutePath().normalize();
        try {
            if (Files.exists(directory) && !Files.isDirectory(directory)) {
                throw new IllegalStateException("Configured document storage path is not a directory: " + directory);
            }
            Files.createDirectories(directory);
            Path probe = Files.createTempFile(directory, ".lsototalbouw-storage-check-", ".tmp");
            Files.deleteIfExists(probe);
        } catch (IOException ex) {
            throw new UncheckedIOException("Production document storage is not writable: " + directory, ex);
        }
    }
}
