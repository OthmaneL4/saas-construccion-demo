package com.lsototalbouw.common.enums;

/**
 * Represents the status of a commercial proposal or quotation sent to a customer.
 */
public enum QuotationStatus {
    /** The quotation is a draft and is not yet sent to the customer. */
    DRAFT("Borrador"),

    /** The quotation has been finalized and sent to the client for consideration. */
    SENT("Enviado"),

    /** The customer has formally accepted the quotation. */
    ACCEPTED("Aceptado"),

    /** The customer has declined or rejected the quotation. */
    REJECTED("Rechazado"),

    /** The quotation validity period has passed without response. */
    EXPIRED("Caducado");

    private final String label;

    QuotationStatus(String label) {
        this.label = label;
    }

    /**
     * Retrieves the user-friendly Spanish label associated with the quotation status.
     *
     * @return the localized display name
     */
    public String getLabel() {
        return label;
    }
}
