package com.lsototalbouw.calendar;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.project.ProjectRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CalendarEventController {

    private final CalendarEventRepository events;
    private final CalendarEventService eventService;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public CalendarEventController(CalendarEventRepository events, CalendarEventService eventService,
                                   ProjectRepository projects, CompanyContextService companyContext,
                                   AuditService auditService) {
        this.events = events;
        this.eventService = eventService;
        this.projects = projects;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/calendar")
    public String index(Model model, @ModelAttribute("eventForm") CalendarEventForm eventForm) {
        prepareModel(model);
        model.addAttribute("eventForm", eventForm);
        return "calendar/index";
    }

    @PostMapping("/calendar")
    public String create(@Valid @ModelAttribute("eventForm") CalendarEventForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareModel(model);
            return "calendar/index";
        }
        CalendarEvent event = eventService.create(form);
        auditService.record(AuditAction.CREATE, "Calendario", event.getId(),
                "Evento creado: " + event.getTitle());
        redirectAttributes.addFlashAttribute("successMessage", "Evento creado correctamente.");
        return "redirect:/calendar";
    }

    @GetMapping("/calendar/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Evento");
        model.addAttribute("event", eventService.getCurrentCompanyEvent(id));
        return "calendar/detail";
    }

    @GetMapping("/calendar/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        CalendarEvent event = eventService.getCurrentCompanyEvent(id);
        prepareModel(model);
        model.addAttribute("pageTitle", "Editar evento");
        model.addAttribute("event", event);
        model.addAttribute("eventForm", CalendarEventForm.from(event));
        return "calendar/edit";
    }

    @PostMapping("/calendar/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("eventForm") CalendarEventForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("event", eventService.getCurrentCompanyEvent(id));
            prepareModel(model);
            model.addAttribute("pageTitle", "Editar evento");
            return "calendar/edit";
        }
        CalendarEvent event = eventService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Calendario", id,
                "Evento actualizado: " + event.getTitle());
        redirectAttributes.addFlashAttribute("successMessage", "Evento actualizado correctamente.");
        return "redirect:/calendar/{id}";
    }

    @PostMapping("/calendar/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CalendarEvent event = eventService.getCurrentCompanyEvent(id);
        eventService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Calendario", id,
                "Evento archivado: " + event.getTitle());
        redirectAttributes.addFlashAttribute("successMessage", "Evento archivado correctamente.");
        return "redirect:/calendar";
    }

    private void prepareModel(Model model) {
        Long companyId = companyContext.currentCompanyId();
        var companyEvents = events.findByCompanyAccountIdAndActiveTrueOrderByEventDateAsc(companyId);
        model.addAttribute("pageTitle", "Calendario");
        model.addAttribute("events", companyEvents);
        model.addAttribute("summary", CalendarEventSummary.from(companyEvents, LocalDate.now()));
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("types", CalendarEventType.values());
    }
}
