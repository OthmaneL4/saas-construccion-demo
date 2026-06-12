package com.lsototalbouw.invoice;

public enum InvoicePaymentReminderStatus {
    REGISTERED("Registrada"),
    GENERATED("Generada");

    private final String label;

    InvoicePaymentReminderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
