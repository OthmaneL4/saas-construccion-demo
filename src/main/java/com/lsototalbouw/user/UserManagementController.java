package com.lsototalbouw.user;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC controller handling user account administration and role allocations.
 *
 * <p>Restricted to company administrators (typically {@code OWNER} or {@code ADMIN}), offering endpoints
 * to list, create, edit, archive (soft-delete), and manually unlock security accounts.
 */
@Controller
public class UserManagementController {

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final UserManagementService userManagementService;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public UserManagementController(AppUserRepository users, RoleRepository roles,
                                    UserManagementService userManagementService,
                                    CompanyContextService companyContext, AuditService auditService) {
        this.users = users;
        this.roles = roles;
        this.userManagementService = userManagementService;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    /**
     * Renders the master user administration control board.
     *
     * @param model    the Spring MVC UI model
     * @param userForm an empty form DTO to bind user registration fields
     * @return logical template view name {@code "users/index"}
     */
    @GetMapping("/users")
    public String index(Model model, @ModelAttribute("userForm") UserManagementForm userForm) {
        addIndexData(model);
        model.addAttribute("userForm", userForm);
        return "users/index";
    }

    /**
     * Processes submission of a new user profile registration form.
     *
     * <p>Saves the new user under the current company scope and logs a creation audit trace.
     *
     * @param form               the validated user form DTO
     * @param bindingResult      the result holder containing field validation errors
     * @param model              the Spring MVC UI model
     * @param redirectAttributes flash attributes holder for success notices
     * @return redirect path to user manager, or view path to register page if validation fails
     */
    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("userForm") UserManagementForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        userManagementService.validateCreate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            addIndexData(model);
            return "users/index";
        }
        AppUser user = userManagementService.create(form);
        auditService.record(AuditAction.CREATE, "Usuarios", user.getId(),
                "Usuario creado: " + user.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Usuario creado correctamente.");
        return "redirect:/users";
    }

    /**
     * Renders the user account editing screen.
     *
     * @param id    the unique identifier of the user to edit
     * @param model the Spring MVC UI model
     * @return logical template view name {@code "users/edit"}
     */
    @GetMapping("/users/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        AppUser user = userManagementService.getCurrentCompanyUser(id);
        model.addAttribute("pageTitle", "Editar usuario");
        model.addAttribute("user", user);
        model.addAttribute("userForm", UserManagementForm.from(user));
        model.addAttribute("roles", roles.findAll());
        return "users/edit";
    }

    /**
     * Commits edits applied to an existing user account.
     *
     * @param id                 the unique ID of the user to update
     * @param form               the validated user parameters form DTO
     * @param bindingResult      the result holder containing field validation errors
     * @param model              the Spring MVC UI model
     * @param redirectAttributes flash attributes holder for success notices
     * @return redirect path to user manager, or view path to edit screen if validation fails
     */
    @PostMapping("/users/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("userForm") UserManagementForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        userManagementService.validateUpdate(id, form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar usuario");
            model.addAttribute("user", userManagementService.getCurrentCompanyUser(id));
            model.addAttribute("roles", roles.findAll());
            return "users/edit";
        }
        AppUser user = userManagementService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Usuarios", id,
                "Usuario actualizado: " + user.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Usuario actualizado correctamente.");
        return "redirect:/users";
    }

    /**
     * Archives (soft-deletes) an active user account.
     *
     * @param id                 the unique ID of the user to archive
     * @param redirectAttributes flash attributes holder for success notices
     * @return redirect path to the user manager list view
     */
    @PostMapping("/users/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userManagementService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Usuarios", id, "Usuario archivado");
        redirectAttributes.addFlashAttribute("successMessage", "Usuario archivado correctamente.");
        return "redirect:/users";
    }

    /**
     * Manually overrides and removes security lock restrictions from a locked user account.
     *
     * @param id                 the unique ID of the user to unlock
     * @param redirectAttributes flash attributes holder for success notices
     * @return redirect path to the user manager list view
     */
    @PostMapping("/users/{id}/unlock")
    public String unlock(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        AppUser user = userManagementService.unlockLogin(id);
        auditService.record(AuditAction.ACCOUNT_UNLOCKED, "Usuarios", id,
                "Bloqueo de login eliminado: " + user.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Usuario desbloqueado correctamente.");
        return "redirect:/users";
    }

    /**
     * Aggregates index page variables (user list and available roles database records) into the UI model.
     */
    private void addIndexData(Model model) {
        List<AppUser> companyUsers = users.findByCompanyAccountIdOrderByFullNameAsc(companyContext.currentCompanyId());
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("users", companyUsers);
        model.addAttribute("summary", UserSecuritySummary.from(companyUsers));
        model.addAttribute("roles", roles.findAll());
    }
}
