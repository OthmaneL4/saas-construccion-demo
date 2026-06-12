package com.lsototalbouw.tool;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.enums.ToolStatus;
import com.lsototalbouw.company.CompanyContextService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
public class ToolController {

    private final ToolRepository tools;
    private final ToolService toolService;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public ToolController(ToolRepository tools, ToolService toolService, CompanyContextService companyContext,
                          AuditService auditService) {
        this.tools = tools;
        this.toolService = toolService;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/tools")
    public String index(Model model,
                        @ModelAttribute("toolForm") ToolForm toolForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "status", required = false) ToolStatus status,
                        @RequestParam(name = "maintenanceDue", defaultValue = "false") boolean maintenanceDue) {
        addIndexData(model, query, status, maintenanceDue);
        model.addAttribute("toolForm", toolForm);
        return "tools/index";
    }

    @PostMapping("/tools")
    public String create(@Valid @ModelAttribute("toolForm") ToolForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, null, false);
            return "tools/index";
        }
        ToolItem tool = toolService.create(form);
        auditService.record(AuditAction.CREATE, "Herramientas", tool.getId(),
                "Herramienta creada: " + tool.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Herramienta creada correctamente.");
        return "redirect:/tools";
    }

    @GetMapping("/tools/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Herramienta");
        model.addAttribute("tool", toolService.getCurrentCompanyTool(id));
        return "tools/detail";
    }

    @GetMapping("/tools/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        ToolItem tool = toolService.getCurrentCompanyTool(id);
        model.addAttribute("pageTitle", "Editar herramienta");
        model.addAttribute("tool", tool);
        model.addAttribute("toolForm", ToolForm.from(tool));
        model.addAttribute("statuses", ToolStatus.values());
        return "tools/edit";
    }

    @PostMapping("/tools/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("toolForm") ToolForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar herramienta");
            model.addAttribute("tool", toolService.getCurrentCompanyTool(id));
            model.addAttribute("statuses", ToolStatus.values());
            return "tools/edit";
        }
        ToolItem tool = toolService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Herramientas", id,
                "Herramienta actualizada: " + tool.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Herramienta actualizada correctamente.");
        return "redirect:/tools/{id}";
    }

    @PostMapping("/tools/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ToolItem tool = toolService.getCurrentCompanyTool(id);
        toolService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Herramientas", id,
                "Herramienta archivada: " + tool.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Herramienta archivada correctamente.");
        return "redirect:/tools";
    }

    private void addIndexData(Model model, String query, ToolStatus status, boolean maintenanceDue) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        LocalDate today = LocalDate.now();
        var toolItems = cleanQuery == null && status == null && !maintenanceDue
                ? tools.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId)
                : tools.searchByCompanyId(companyId, cleanQuery, status, maintenanceDue, today);
        model.addAttribute("pageTitle", "Herramientas");
        model.addAttribute("tools", toolItems);
        model.addAttribute("summary", ToolInventorySummary.from(toolItems, today));
        model.addAttribute("statuses", ToolStatus.values());
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("maintenanceDue", maintenanceDue);
    }
}
