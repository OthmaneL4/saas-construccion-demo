package com.lsototalbouw.common.enums;

/**
 * Represents the lifecycle status of a customer invoice.
 */
public enum InvoiceStatus {
    /** Invoice is a work in progress and has not been finalized or sent to the customer yet. */
    DRAFT("Borrador"),

    /** Invoice has been finalized and dispatched to the customer. */
    SENT("Enviada"),

    /** Invoice has received one or more payments, but the outstanding balance is still greater than zero. */
    PARTIALLY_PAID("Parcialmente pagada"),

    /** Invoice is fully paid, with no remaining outstanding balance. */
    PAID("Pagada"),

    /** Invoice is past its due date and has not been fully paid. */
    OVERDUE("Vencida"),

    /** Invoice has been cancelled or voided. */
    CANCELLED("Cancelada");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    /**
     * Retrieves the user-friendly Spanish label associated with the status.
     *
     * @return the localized display name
     */
    public String getLabel() {
        return label;
    }
}
