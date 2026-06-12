package com.lsototalbouw.invoice;

public enum InvoicePaymentReminderType {
    MANUAL("Manual"),
    PDF("PDF");

    private final String label;

    InvoicePaymentReminderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
