package com.lsototalbouw.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reports operational health for the document storage directory.
 *
 * <p>The application stores uploaded documents on disk. This health indicator allows operational
 * monitoring to detect missing, invalid, or non-writable storage after startup.
 */
@Component("documentStorageHealthIndicator")
public class DocumentStorageHealthIndicator implements HealthIndicator {

    private final String documentsDir;

    public DocumentStorageHealthIndicator(@Value("${app.upload.documents-dir:uploads/documents}") String documentsDir) {
        this.documentsDir = documentsDir;
    }

    @Override
    public Health health() {
        if (!StringUtils.hasText(documentsDir)) {
            return Health.down()
                    .withDetail("reason", "Document storage directory is not configured")
                    .build();
        }
        Path directory = Paths.get(documentsDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            return Health.down()
                    .withDetail("path", directory.toString())
                    .withDetail("reason", "Document storage path is not a directory")
                    .build();
        }
        try {
            Path probe = Files.createTempFile(directory, ".lsototalbouw-health-", ".tmp");
            Files.deleteIfExists(probe);
            return Health.up()
                    .withDetail("path", directory.toString())
                    .build();
        } catch (IOException ex) {
            return Health.down(ex)
                    .withDetail("path", directory.toString())
                    .withDetail("reason", "Document storage directory is not writable")
                    .build();
        }
    }
}
