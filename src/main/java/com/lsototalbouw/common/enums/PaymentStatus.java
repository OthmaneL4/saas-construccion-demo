package com.lsototalbouw.common.enums;

/**
 * Represents the execution state of an invoice payment.
 */
public enum PaymentStatus {
    /** Payment is expected or registered but not yet cleared. */
    PENDING("Pendiente"),

    /** Payment has been successfully processed and confirmed. */
    COMPLETED("Confirmado"),

    /** Payment attempt failed. */
    FAILED("Fallido"),

    /** Payment has been refunded back to the customer. */
    REFUNDED("Devuelto");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    /**
     * Retrieves the user-friendly Spanish label associated with the payment status.
     *
     * @return the localized display name
     */
    public String getLabel() {
        return label;
    }
}
