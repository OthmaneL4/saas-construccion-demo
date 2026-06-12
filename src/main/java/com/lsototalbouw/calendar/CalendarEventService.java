package com.lsototalbouw.calendar;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarEventService {

    private final CalendarEventRepository events;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;

    public CalendarEventService(CalendarEventRepository events, ProjectRepository projects,
                                CompanyContextService companyContext) {
        this.events = events;
        this.projects = projects;
        this.companyContext = companyContext;
    }

    @Transactional(readOnly = true)
    public CalendarEvent getCurrentCompanyEvent(Long id) {
        return events.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
    }

    @Transactional
    public CalendarEvent create(CalendarEventForm form) {
        Long companyId = companyContext.currentCompanyId();
        Project project = findProject(companyId, form.getProjectId());
        CalendarEvent event = new CalendarEvent(
                companyContext.currentCompany(),
                project,
                form.getTitle().trim(),
                clean(form.getNotes()),
                form.getEventDate(),
                form.getType()
        );
        return events.save(event);
    }

    @Transactional
    public CalendarEvent update(Long id, CalendarEventForm form) {
        Long companyId = companyContext.currentCompanyId();
        CalendarEvent event = getCurrentCompanyEvent(id);
        Project project = findProject(companyId, form.getProjectId());
        event.updateFrom(project, form.getTitle().trim(), clean(form.getNotes()), form.getEventDate(), form.getType());
        return event;
    }

    @Transactional
    public void archive(Long id) {
        CalendarEvent event = getCurrentCompanyEvent(id);
        event.setActive(false);
    }

    private Project findProject(Long companyId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
