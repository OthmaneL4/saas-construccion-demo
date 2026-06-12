package com.lsototalbouw.project;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.enums.ProjectStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.document.BusinessDocument;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.document.DocumentService;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.profitability.ProfitabilityService;
import com.lsototalbouw.timesheet.WorkLogRepository;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class ProjectController {

    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final CustomerRepository customers;
    private final CompanyContextService companyContext;
    private final BusinessDocumentRepository documents;
    private final DocumentService documentService;
    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;
    private final WorkLogRepository workLogs;
    private final ProfitabilityService profitabilityService;
    private final AuditService auditService;

    public ProjectController(ProjectRepository projects, ProjectService projectService, CustomerRepository customers,
                              CompanyContextService companyContext, BusinessDocumentRepository documents,
                              DocumentService documentService, InvoiceRepository invoices, ExpenseRepository expenses,
                              WorkLogRepository workLogs, ProfitabilityService profitabilityService,
                              AuditService auditService) {
        this.projects = projects;
        this.projectService = projectService;
        this.customers = customers;
        this.companyContext = companyContext;
        this.documents = documents;
        this.documentService = documentService;
        this.invoices = invoices;
        this.expenses = expenses;
        this.workLogs = workLogs;
        this.profitabilityService = profitabilityService;
        this.auditService = auditService;
    }

    @GetMapping("/projects")
    public String index(Model model, @ModelAttribute("projectForm") ProjectForm projectForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "status", required = false) ProjectStatus status,
                        @RequestParam(name = "page", defaultValue = "0") int page) {
        Long companyId = companyContext.currentCompanyId();
        addIndexData(model, query, status, page);
        model.addAttribute("projectForm", projectForm);
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        return "projects/index";
    }

    @PostMapping("/projects")
    public String create(@Valid @ModelAttribute("projectForm") ProjectForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Long companyId = companyContext.currentCompanyId();
            addIndexData(model, null, null, 0);
            model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
            return "projects/index";
        }
        Project project = projectService.create(form);
        auditService.record(AuditAction.CREATE, "Proyectos", project.getId(),
                "Proyecto creado: " + project.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proyecto creado correctamente.");
        return "redirect:/projects";
    }

    @GetMapping("/projects/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        List<BusinessDocument> projectDocuments = documents.findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByCreatedAtDesc(
                companyId, id);
        model.addAttribute("pageTitle", "Proyecto");
        model.addAttribute("project", projectService.getCurrentCompanyProject(id));
        model.addAttribute("documents", projectDocuments);
        model.addAttribute("fileAvailability", fileAvailability(projectDocuments));
        model.addAttribute("profitability", profitabilityService.currentCompanyProjectRow(id));
        model.addAttribute("projectInvoices", invoices.findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByDueDateAsc(
                companyId, id));
        model.addAttribute("projectExpenses", expenses.findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByExpenseDateDesc(
                companyId, id));
        model.addAttribute("projectWorkLogs", workLogs.findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByWorkDateDescCreatedAtDesc(
                companyId, id));
        return "projects/detail";
    }

    private Map<Long, Boolean> fileAvailability(List<BusinessDocument> relatedDocuments) {
        Map<Long, Boolean> availability = new LinkedHashMap<>();
        for (BusinessDocument document : relatedDocuments) {
            availability.put(document.getId(), documentService.isFileAvailable(document));
        }
        return availability;
    }

    @GetMapping("/projects/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        Project project = projectService.getCurrentCompanyProject(id);
        model.addAttribute("pageTitle", "Editar proyecto");
        model.addAttribute("project", project);
        model.addAttribute("projectForm", ProjectForm.from(project));
        model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
        model.addAttribute("statuses", ProjectStatus.values());
        return "projects/edit";
    }

    @PostMapping("/projects/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("projectForm") ProjectForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long companyId = companyContext.currentCompanyId();
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar proyecto");
            model.addAttribute("project", projectService.getCurrentCompanyProject(id));
            model.addAttribute("customers", customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId));
            model.addAttribute("statuses", ProjectStatus.values());
            return "projects/edit";
        }
        Project project = projectService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Proyectos", id,
                "Proyecto actualizado: " + project.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proyecto actualizado correctamente.");
        return "redirect:/projects/{id}";
    }

    @PostMapping("/projects/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Project project = projectService.getCurrentCompanyProject(id);
        projectService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Proyectos", id,
                "Proyecto archivado: " + project.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proyecto archivado correctamente.");
        return "redirect:/projects";
    }

    private void addIndexData(Model model, String query, ProjectStatus status, int page) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("startDate").descending());
        model.addAttribute("pageTitle", "Proyectos");
        model.addAttribute("projectPage", cleanQuery == null && status == null
                ? projects.findPageByCompanyId(companyId, pageable)
                : projects.searchPageByCompanyId(companyId, cleanQuery, status, pageable));
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ProjectStatus.values());
    }
}
