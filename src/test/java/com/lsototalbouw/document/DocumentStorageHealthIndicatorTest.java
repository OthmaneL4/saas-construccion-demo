package com.lsototalbouw.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class DocumentStorageHealthIndicatorTest {

    @TempDir
    private Path tempDir;

    @Test
    void reportsUpWhenDocumentStorageDirectoryIsWritable() {
        DocumentStorageHealthIndicator indicator = new DocumentStorageHealthIndicator(tempDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("path");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void reportsDownWhenDocumentStorageDirectoryIsMissing() {
        Path missingDirectory = tempDir.resolve("missing-documents");
        DocumentStorageHealthIndicator indicator = new DocumentStorageHealthIndicator(missingDirectory.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Document storage path is not a directory");
    }

    @Test
    void reportsDownWhenDocumentStoragePathIsAFile() throws Exception {
        Path filePath = tempDir.resolve("documents-file");
        Files.writeString(filePath, "not a directory");
        DocumentStorageHealthIndicator indicator = new DocumentStorageHealthIndicator(filePath.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Document storage path is not a directory");
    }
}
