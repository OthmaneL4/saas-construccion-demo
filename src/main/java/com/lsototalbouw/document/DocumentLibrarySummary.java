package com.lsototalbouw.document;

import java.util.List;
import java.util.Locale;

public record DocumentLibrarySummary(
        int totalDocuments,
        long totalFileSize,
        int imageDocuments,
        int pdfDocuments,
        int unlinkedDocuments,
        int unavailableDocuments
) {

    public static DocumentLibrarySummary from(List<BusinessDocument> documents) {
        return from(documents, java.util.Map.of());
    }

    public static DocumentLibrarySummary from(List<BusinessDocument> documents, java.util.Map<Long, Boolean> fileAvailability) {
        int imageDocuments = 0;
        int pdfDocuments = 0;
        int unlinkedDocuments = 0;
        int unavailableDocuments = 0;
        long totalFileSize = 0;

        for (BusinessDocument document : documents) {
            totalFileSize += document.getFileSize();
            if (document.isImage()) {
                imageDocuments++;
            }
            if (document.isPdf()) {
                pdfDocuments++;
            }
            if (document.getCustomer() == null && document.getProject() == null) {
                unlinkedDocuments++;
            }
            if (Boolean.FALSE.equals(fileAvailability.get(document.getId()))) {
                unavailableDocuments++;
            }
        }

        return new DocumentLibrarySummary(
                documents.size(),
                totalFileSize,
                imageDocuments,
                pdfDocuments,
                unlinkedDocuments,
                unavailableDocuments
        );
    }

    public String readableTotalFileSize() {
        if (totalFileSize >= 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f MB", totalFileSize / 1024.0 / 1024.0);
        }
        if (totalFileSize >= 1024) {
            return String.format(Locale.ROOT, "%.1f KB", totalFileSize / 1024.0);
        }
        return totalFileSize + " B";
    }
}
