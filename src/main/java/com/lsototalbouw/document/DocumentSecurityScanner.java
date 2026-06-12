package com.lsototalbouw.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Performs local defensive checks before a stored document becomes available to the application.
 *
 * <p>This component is intentionally small and deterministic. It does not replace a production antivirus engine, but
 * it creates the integration point where a ClamAV or managed malware scanning provider can be added later without
 * changing the document upload flow.
 */
@Component
public class DocumentSecurityScanner {

    private static final int MAX_SIGNATURE_SCAN_BYTES = 1024 * 1024;
    private static final List<String> BLOCKED_SIGNATURES = List.of(
            "eicar-standard-antivirus-test-file",
            "<script",
            "<?php",
            "<%@",
            "powershell -",
            "cmd.exe",
            "/bin/sh"
    );

    /**
     * Scans a stored upload for high-risk signatures.
     *
     * @param file             stored file path inside the controlled document directory
     * @param originalFilename original user-facing filename, used only for error context
     * @param contentType      normalized document content type
     * @throws IllegalArgumentException when the file is considered unsafe
     */
    public void scan(Path file, String originalFilename, String contentType) {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException("El archivo no esta disponible para escaneo de seguridad.");
        }
        String sample = readSample(file).toLowerCase(Locale.ROOT);
        for (String signature : BLOCKED_SIGNATURES) {
            if (sample.contains(signature)) {
                throw new IllegalArgumentException(
                        "El archivo no ha superado el escaneo de seguridad. Revisa el documento antes de subirlo.");
            }
        }
    }

    private String readSample(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] bytes = inputStream.readNBytes(MAX_SIGNATURE_SCAN_BYTES);
            return new String(bytes, StandardCharsets.ISO_8859_1);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo escanear el archivo", ex);
        }
    }
}
