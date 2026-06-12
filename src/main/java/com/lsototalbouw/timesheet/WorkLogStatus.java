package com.lsototalbouw.timesheet;

public enum WorkLogStatus {
    DRAFT("Borrador"),
    APPROVED("Aprobado"),
    INVOICED("Facturado");

    private final String label;

    WorkLogStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
