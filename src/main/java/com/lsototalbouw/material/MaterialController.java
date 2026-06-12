package com.lsototalbouw.material;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import jakarta.validation.Valid;
import java.util.List;
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
public class MaterialController {

    private final MaterialRepository materials;
    private final MaterialService materialService;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public MaterialController(MaterialRepository materials, MaterialService materialService,
                              CompanyContextService companyContext, AuditService auditService) {
        this.materials = materials;
        this.materialService = materialService;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/materials")
    public String index(Model model, @ModelAttribute("materialForm") MaterialForm materialForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "lowStock", defaultValue = "false") boolean lowStock) {
        addIndexData(model, query, lowStock);
        model.addAttribute("materialForm", materialForm);
        return "materials/index";
    }

    @PostMapping("/materials")
    public String create(@Valid @ModelAttribute("materialForm") MaterialForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, false);
            return "materials/index";
        }
        MaterialItem material = materialService.create(form);
        auditService.record(AuditAction.CREATE, "Materiales", material.getId(),
                "Material creado: " + material.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Material creado correctamente.");
        return "redirect:/materials";
    }

    @GetMapping("/materials/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("pageTitle", "Material");
        model.addAttribute("material", materialService.getCurrentCompanyMaterial(id));
        return "materials/detail";
    }

    @GetMapping("/materials/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        MaterialItem material = materialService.getCurrentCompanyMaterial(id);
        model.addAttribute("pageTitle", "Editar material");
        model.addAttribute("material", material);
        model.addAttribute("materialForm", MaterialForm.from(material));
        return "materials/edit";
    }

    @PostMapping("/materials/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("materialForm") MaterialForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar material");
            model.addAttribute("material", materialService.getCurrentCompanyMaterial(id));
            return "materials/edit";
        }
        MaterialItem material = materialService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Materiales", id,
                "Material actualizado: " + material.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Material actualizado correctamente.");
        return "redirect:/materials/{id}";
    }

    @PostMapping("/materials/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        MaterialItem material = materialService.getCurrentCompanyMaterial(id);
        materialService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Materiales", id,
                "Material archivado: " + material.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Material archivado correctamente.");
        return "redirect:/materials";
    }

    private void addIndexData(Model model, String query, boolean lowStock) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        List<MaterialItem> materialItems = cleanQuery == null && !lowStock
                ? materials.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companyId)
                : materials.searchByCompanyId(companyId, cleanQuery, lowStock);
        model.addAttribute("pageTitle", "Materiales");
        model.addAttribute("materials", materialItems);
        model.addAttribute("summary", MaterialInventorySummary.from(materialItems));
        model.addAttribute("query", cleanQuery);
        model.addAttribute("lowStock", lowStock);
    }
}
