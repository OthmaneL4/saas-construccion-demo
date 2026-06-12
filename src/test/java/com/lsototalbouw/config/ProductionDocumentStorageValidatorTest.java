package com.lsototalbouw.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductionDocumentStorageValidatorTest {

    @TempDir
    private Path tempDir;

    @Test
    void failsFastWhenProductionDocumentDirectoryIsMissing() {
        ProductionDocumentStorageValidator validator = new ProductionDocumentStorageValidator("");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOCUMENTS_DIR");
    }

    @Test
    void failsFastWhenProductionDocumentPathIsAFile() throws Exception {
        Path filePath = tempDir.resolve("documents-file");
        Files.writeString(filePath, "not a directory");
        ProductionDocumentStorageValidator validator = new ProductionDocumentStorageValidator(filePath.toString());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a directory");
    }

    @Test
    void createsAndVerifiesWritableProductionDocumentDirectory() {
        Path documentsDir = tempDir.resolve("documents");
        ProductionDocumentStorageValidator validator = new ProductionDocumentStorageValidator(documentsDir.toString());

        validator.validate();

        assertThat(documentsDir).isDirectory();
        assertThat(documentsDir).isEmptyDirectory();
    }
}
