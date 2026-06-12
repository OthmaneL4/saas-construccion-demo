package com.lsototalbouw.calendar;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "calendar_events", indexes = {
        @Index(name = "idx_calendar_company_active_date", columnList = "company_account_id, active, event_date"),
        @Index(name = "idx_calendar_project", columnList = "project_id")
})
public class CalendarEvent extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String notes;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CalendarEventType type;

    protected CalendarEvent() {
    }

    public CalendarEvent(CompanyAccount companyAccount, Project project, String title, String notes,
                         LocalDate eventDate, CalendarEventType type) {
        this.companyAccount = companyAccount;
        this.project = project;
        this.title = title;
        this.notes = notes;
        this.eventDate = eventDate;
        this.type = type;
    }

    public void updateFrom(Project project, String title, String notes, LocalDate eventDate, CalendarEventType type) {
        this.project = project;
        this.title = title;
        this.notes = notes;
        this.eventDate = eventDate;
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public CalendarEventType getType() {
        return type;
    }
}
