package com.lsototalbouw.calendar;

public enum CalendarEventType {
    WORK("Trabajo"),
    VISIT("Visita"),
    DEADLINE("Vencimiento"),
    REMINDER("Recordatorio"),
    MAINTENANCE("Mantenimiento");

    private final String label;

    CalendarEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
