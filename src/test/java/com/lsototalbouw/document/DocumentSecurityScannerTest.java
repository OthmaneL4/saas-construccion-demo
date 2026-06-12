package com.lsototalbouw.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentSecurityScannerTest {

    @TempDir
    private Path tempDir;

    private final DocumentSecurityScanner scanner = new DocumentSecurityScanner();

    @Test
    void rejectsEicarSignatureBeforeDocumentBecomesAvailable() throws Exception {
        Path file = tempDir.resolve("invoice.pdf");
        Files.writeString(file, "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*");

        assertThatThrownBy(() -> scanner.scan(file, "invoice.pdf", "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escaneo de seguridad");
    }

    @Test
    void acceptsOrdinaryPdfContent() throws Exception {
        Path file = tempDir.resolve("invoice.pdf");
        Files.writeString(file, "%PDF-1.7\nFactura LSOTOTALBOUW\n%%EOF");

        scanner.scan(file, "invoice.pdf", "application/pdf");
    }
}
