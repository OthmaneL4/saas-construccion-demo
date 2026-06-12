package com.lsototalbouw.common.enums;

/**
 * Represents the execution status of a building or renovation project.
 */
public enum ProjectStatus {
    /** Project has been created and scheduled, but physical work has not yet commenced. */
    PLANNED("Planificado"),

    /** Active construction or implementation is currently underway. */
    IN_PROGRESS("En curso"),

    /** Work has been temporarily suspended, awaiting materials, permits, or client feedback. */
    ON_HOLD("Pausado"),

    /** All construction activities are finished and the project is signed off by the customer. */
    COMPLETED("Finalizado"),

    /** The project contract or scope was cancelled. */
    CANCELLED("Cancelado");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    /**
     * Retrieves the user-friendly Spanish label associated with the project status.
     *
     * @return the localized display name
     */
    public String getLabel() {
        return label;
    }
}
