package com.lsototalbouw.calendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CalendarEventForm {

    private Long projectId;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 160)
    private String title;

    @Size(max = 500)
    private String notes;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate eventDate = LocalDate.now();

    @NotNull
    private CalendarEventType type = CalendarEventType.WORK;

    public static CalendarEventForm from(CalendarEvent event) {
        CalendarEventForm form = new CalendarEventForm();
        form.setProjectId(event.getProject() != null ? event.getProject().getId() : null);
        form.setTitle(event.getTitle());
        form.setNotes(event.getNotes());
        form.setEventDate(event.getEventDate());
        form.setType(event.getType());
        return form;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public CalendarEventType getType() {
        return type;
    }

    public void setType(CalendarEventType type) {
        this.type = type;
    }
}
