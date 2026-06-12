package com.lsototalbouw.company;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC controller handling the user interface settings page for the tenant company profile.
 *
 * <p>Restricted to users with higher authorization levels (typically {@code OWNER} or {@code ADMIN}),
 * managing endpoints to load and update settings properties.
 */
@Controller
public class CompanySettingsController {

    private final CompanySettingsService settingsService;

    public CompanySettingsController(CompanySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Renders the company profile settings editing view.
     *
     * <p>Populates the model with the current company account details form.
     *
     * @param model the Spring MVC UI model
     * @return the logical view path name {@code "settings/company"}
     */
    @GetMapping("/settings/company")
    public String edit(Model model) {
        if (!model.containsAttribute("companySettingsForm")) {
            model.addAttribute("companySettingsForm", settingsService.currentSettings());
        }
        model.addAttribute("pageTitle", "Empresa");
        return "settings/company";
    }

    /**
     * Processes submission of updated company settings, enforcing validation.
     *
     * <p>If validation failures exist, returns the user to the edit page with validation errors highlighted.
     * Otherwise, commits settings changes and redirects back with a success banner.
     *
     * @param form               the validated settings form DTO
     * @param bindingResult      the result holder containing any field validation errors
     * @param model              the Spring MVC UI model (used if validation fails)
     * @param redirectAttributes flash attributes holder used for transient success messages after redirect
     * @return redirect path to the company settings edit page on success, or view path to edit page on validation failure
     */
    @PostMapping("/settings/company")
    public String update(@Valid @ModelAttribute("companySettingsForm") CompanySettingsForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Empresa");
            return "settings/company";
        }
        settingsService.update(form);
        redirectAttributes.addFlashAttribute("success", "Datos de empresa actualizados correctamente.");
        return "redirect:/settings/company";
    }
}
