package com.lsototalbouw.timesheet;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.project.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkLogController {

    private final WorkLogRepository workLogs;
    private final WorkLogService workLogService;
    private final WorkLogInvoiceService workLogInvoiceService;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public WorkLogController(WorkLogRepository workLogs, WorkLogService workLogService,
                             WorkLogInvoiceService workLogInvoiceService, ProjectRepository projects,
                             InvoiceRepository invoices, CompanyContextService companyContext,
                             AuditService auditService) {
        this.workLogs = workLogs;
        this.workLogService = workLogService;
        this.workLogInvoiceService = workLogInvoiceService;
        this.projects = projects;
        this.invoices = invoices;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/work-logs")
    public String index(Model model,
                        @ModelAttribute("workLogForm") WorkLogForm workLogForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "projectId", required = false) Long projectId,
                        @RequestParam(name = "status", required = false) WorkLogStatus status,
                        @RequestParam(name = "billable", required = false) Boolean billable) {
        addIndexData(model, query, projectId, status, billable);
        model.addAttribute("workLogForm", workLogForm);
        return "work-logs/index";
    }

    @PostMapping("/work-logs")
    public String create(@Valid @ModelAttribute("workLogForm") WorkLogForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, null, null, null);
            return "work-logs/index";
        }
        WorkLog workLog = workLogService.create(form);
        auditService.record(AuditAction.CREATE, "Partes de horas", workLog.getId(),
                "Parte de horas creado", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Parte de horas creado correctamente.");
        return "redirect:/work-logs";
    }

    @GetMapping("/work-logs/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        WorkLog workLog = workLogService.getCurrentCompanyWorkLog(id);
        model.addAttribute("pageTitle", "Parte de horas");
        model.addAttribute("workLog", workLog);
        model.addAttribute("workLogInvoiceForm", new WorkLogInvoiceForm());
        model.addAttribute("invoices", invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyContext.currentCompanyId()));
        return "work-logs/detail";
    }

    @PostMapping("/work-logs/{id}/invoice-line")
    public String convertToInvoiceLine(@PathVariable("id") Long id,
                                       @Valid @ModelAttribute("workLogInvoiceForm") WorkLogInvoiceForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        WorkLog workLog = workLogService.getCurrentCompanyWorkLog(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Parte de horas");
            model.addAttribute("workLog", workLog);
            model.addAttribute("invoices", invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyContext.currentCompanyId()));
            return "work-logs/detail";
        }
        try {
            workLogInvoiceService.convertToInvoiceLine(id, form.getInvoiceId());
            auditService.record(AuditAction.CONVERT, "Partes de horas", id,
                    "Parte convertido en linea de factura", "Factura " + form.getInvoiceId());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("workLog.invoiceLine.invalid", ex.getMessage());
            model.addAttribute("pageTitle", "Parte de horas");
            model.addAttribute("workLog", workLog);
            model.addAttribute("invoices", invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyContext.currentCompanyId()));
            return "work-logs/detail";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Parte convertido en linea de factura correctamente.");
        return "redirect:/work-logs/{id}";
    }

    @GetMapping("/work-logs/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        WorkLog workLog = workLogService.getCurrentCompanyWorkLog(id);
        model.addAttribute("pageTitle", "Editar parte");
        model.addAttribute("workLog", workLog);
        model.addAttribute("workLogForm", WorkLogForm.from(workLog));
        addReferenceData(model);
        return "work-logs/edit";
    }

    @PostMapping("/work-logs/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("workLogForm") WorkLogForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar parte");
            model.addAttribute("workLog", workLogService.getCurrentCompanyWorkLog(id));
            addReferenceData(model);
            return "work-logs/edit";
        }
        workLogService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Partes de horas", id,
                "Parte de horas actualizado", form.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Parte de horas actualizado correctamente.");
        return "redirect:/work-logs/{id}";
    }

    @PostMapping("/work-logs/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        workLogService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Partes de horas", id, "Parte de horas archivado");
        redirectAttributes.addFlashAttribute("successMessage", "Parte de horas archivado correctamente.");
        return "redirect:/work-logs";
    }

    private void addIndexData(Model model, String query, Long projectId, WorkLogStatus status, Boolean billable) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        var companyWorkLogs = cleanQuery == null && projectId == null && status == null && billable == null
                ? workLogs.findByCompanyAccountIdAndActiveTrueOrderByWorkDateDescCreatedAtDesc(companyId)
                : workLogs.searchByCompanyId(companyId, projectId, status, billable, cleanQuery);
        model.addAttribute("pageTitle", "Partes de horas");
        model.addAttribute("workLogs", companyWorkLogs);
        model.addAttribute("summary", WorkLogSummary.from(companyWorkLogs));
        model.addAttribute("statuses", WorkLogStatus.values());
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedProjectId", projectId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBillable", billable);
        addReferenceData(model);
    }

    private void addReferenceData(Model model) {
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyContext.currentCompanyId()));
    }
}
