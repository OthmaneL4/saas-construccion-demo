package com.lsototalbouw.supplier;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import jakarta.validation.Valid;
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
public class SupplierController {

    private final SupplierRepository suppliers;
    private final SupplierService supplierService;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public SupplierController(SupplierRepository suppliers, SupplierService supplierService,
                              CompanyContextService companyContext, AuditService auditService) {
        this.suppliers = suppliers;
        this.supplierService = supplierService;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/suppliers")
    public String index(Model model,
                        @ModelAttribute("supplierForm") SupplierForm supplierForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "page", defaultValue = "0") int page) {
        addIndexData(model, query, page);
        model.addAttribute("supplierForm", supplierForm);
        return "suppliers/index";
    }

    @PostMapping("/suppliers")
    public String create(@Valid @ModelAttribute("supplierForm") SupplierForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addIndexData(model, null, 0);
            return "suppliers/index";
        }
        Supplier supplier = supplierService.create(form);
        auditService.record(AuditAction.CREATE, "Proveedores", supplier.getId(),
                "Proveedor creado: " + supplier.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proveedor creado correctamente.");
        return "redirect:/suppliers";
    }

    @GetMapping("/suppliers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Proveedor");
        model.addAttribute("supplier", supplierService.getCurrentCompanySupplier(id));
        return "suppliers/detail";
    }

    @GetMapping("/suppliers/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getCurrentCompanySupplier(id);
        model.addAttribute("pageTitle", "Editar proveedor");
        model.addAttribute("supplier", supplier);
        model.addAttribute("supplierForm", SupplierForm.from(supplier));
        return "suppliers/edit";
    }

    @PostMapping("/suppliers/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("supplierForm") SupplierForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar proveedor");
            model.addAttribute("supplier", supplierService.getCurrentCompanySupplier(id));
            return "suppliers/edit";
        }
        Supplier supplier = supplierService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Proveedores", supplier.getId(),
                "Proveedor actualizado: " + supplier.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proveedor actualizado correctamente.");
        return "redirect:/suppliers/{id}";
    }

    @PostMapping("/suppliers/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Supplier supplier = supplierService.getCurrentCompanySupplier(id);
        supplierService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Proveedores", id,
                "Proveedor archivado: " + supplier.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Proveedor archivado correctamente.");
        return "redirect:/suppliers";
    }

    private void addIndexData(Model model, String query, int page) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("name").ascending());
        model.addAttribute("pageTitle", "Proveedores");
        model.addAttribute("supplierPage", cleanQuery == null
                ? suppliers.findByCompanyAccountIdAndActiveTrue(companyId, pageable)
                : suppliers.searchByCompanyId(companyId, cleanQuery, pageable));
        model.addAttribute("summary", new SupplierDirectorySummary(
                suppliers.countByCompanyAccountIdAndActiveTrue(companyId),
                suppliers.countWithEmailByCompanyId(companyId),
                suppliers.countWithPhoneByCompanyId(companyId),
                suppliers.countCoveredCitiesByCompanyId(companyId)
        ));
        model.addAttribute("query", cleanQuery);
    }
}
