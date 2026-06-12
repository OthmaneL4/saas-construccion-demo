package com.lsototalbouw.common.enums;

/**
 * Represents the inventory and operational status of a company tool or equipment item.
 */
public enum ToolStatus {
    /** Tool is available in storage and ready for deployment. */
    AVAILABLE("Disponible"),

    /** Tool is currently checked out or assigned to an active project site. */
    IN_USE("En uso"),

    /** Tool is currently undergoing repair or scheduled maintenance calibration. */
    MAINTENANCE("En mantenimiento"),

    /** Tool has been misplaced, lost, or stolen. */
    LOST("Perdida"),

    /** Tool has reached end-of-life and is permanently retired or discarded. */
    RETIRED("Retirada");

    private final String label;

    ToolStatus(String label) {
        this.label = label;
    }

    /**
     * Retrieves the user-friendly Spanish label associated with the tool status.
     *
     * @return the localized display name
     */
    public String getLabel() {
        return label;
    }
}
