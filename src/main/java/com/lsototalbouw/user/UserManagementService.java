package com.lsototalbouw.user;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.notification.BusinessNotification;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

/**
 * Service managing user account administration within a tenant company context.
 *
 * <p>Handles password hashing, role resolution, tenant context validation, duplicate email checking,
 * and user status operations (unlocking locked accounts, archiving/soft-deleting users).
 */
@Service
public class UserManagementService {

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final CompanyContextService companyContext;
    private final PasswordEncoder passwordEncoder;
    private final BusinessNotificationRepository notifications;

    public UserManagementService(AppUserRepository users, RoleRepository roles,
                                 CompanyContextService companyContext, PasswordEncoder passwordEncoder,
                                 BusinessNotificationRepository notifications) {
        this.users = users;
        this.roles = roles;
        this.companyContext = companyContext;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
    }

    /**
     * Validates input details for a new user account registration.
     *
     * <p>Enforces password presence and length constraints, email uniqueness,
     * and role presence checks.
     *
     * @param form          the form containing input data
     * @param bindingResult the binding result holder to register validation errors
     */
    public void validateCreate(UserManagementForm form, BindingResult bindingResult) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "user.password.required", "La contrasena es obligatoria");
        }
        validatePasswordLength(form, bindingResult);
        if (form.getEmail() != null && users.existsByEmailIgnoreCase(form.getEmail().trim())) {
            bindingResult.rejectValue("email", "user.email.duplicate", "Ya existe un usuario con este email");
        }
        validateRoles(form, bindingResult);
    }

    /**
     * Validates input details for updating an existing user account.
     *
     * <p>Ensures that the email does not collide with other existing users, and validates
     * password length (if updated) and role presence.
     *
     * @param id            the unique identifier of the user being updated
     * @param form          the form containing input data
     * @param bindingResult the binding result holder to register validation errors
     */
    public void validateUpdate(Long id, UserManagementForm form, BindingResult bindingResult) {
        if (form.getEmail() != null && users.existsByEmailIgnoreCaseAndIdNot(form.getEmail().trim(), id)) {
            bindingResult.rejectValue("email", "user.email.duplicate", "Ya existe un usuario con este email");
        }
        validatePasswordLength(form, bindingResult);
        validateRoles(form, bindingResult);
    }

    /**
     * Creates and saves a new user account under the active company tenant.
     *
     * <p>Hashes the password using BCrypt before persisting.
     *
     * @param form the form containing the new user parameters
     * @return the newly saved {@link AppUser} entity
     * @throws IllegalStateException if no company tenant is resolved in context
     */
    @Transactional
    public AppUser create(UserManagementForm form) {
        CompanyAccount company = companyContext.currentCompany();
        AppUser user = new AppUser(
                company,
                form.getFullName().trim(),
                form.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(form.getPassword())
        );
        user.getRoles().addAll(resolveRoles(form.getRoleIds()));
        return users.save(user);
    }

    /**
     * Resolves a user account by ID, restricted to the current tenant scope.
     *
     * @param id the user ID
     * @return the matched {@link AppUser} entity
     * @throws IllegalArgumentException if the user cannot be found under the active tenant
     */
    @Transactional(readOnly = true)
    public AppUser getCurrentCompanyUser(Long id) {
        return users.findByCompanyAccountIdAndId(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    /**
     * Updates an existing user's profile and roles.
     *
     * <p>If a new password is provided in the form, it is hashed and updated.
     *
     * @param id   the user ID to update
     * @param form the form containing updated profile parameters
     * @return the updated user entity
     * @throws IllegalArgumentException if the user cannot be resolved under the current tenant
     */
    @Transactional
    public AppUser update(Long id, UserManagementForm form) {
        AppUser user = getCurrentCompanyUser(id);
        user.updateProfile(form.getFullName().trim(), form.getEmail().trim().toLowerCase());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.updatePassword(passwordEncoder.encode(form.getPassword()));
        }
        user.getRoles().clear();
        user.getRoles().addAll(resolveRoles(form.getRoleIds()));
        return user;
    }

    /**
     * Archives (soft-deletes) a user account.
     *
     * @param id the user ID to archive
     * @throws IllegalArgumentException if the user cannot be resolved under the current tenant
     */
    @Transactional
    public void archive(Long id) {
        AppUser user = getCurrentCompanyUser(id);
        user.setActive(false);
    }

    /**
     * Resets failed login counters and unlocks a locked user account.
     *
     * @param id the user ID to unlock
     * @return the unlocked user entity
     * @throws IllegalArgumentException if the user cannot be resolved under the current tenant
     */
    @Transactional
    public AppUser unlockLogin(Long id) {
        AppUser user = getCurrentCompanyUser(id);
        user.unlockLogin();
        notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(
                        companyContext.currentCompanyId(), "security:login-lock:" + user.getId())
                .filter(BusinessNotification::isUnread)
                .ifPresent(BusinessNotification::markAsRead);
        return user;
    }

    /**
     * Enforces that at least one role is selected for a user account.
     */
    private void validateRoles(UserManagementForm form, BindingResult bindingResult) {
        if (form.getRoleIds() == null || form.getRoleIds().isEmpty()) {
            bindingResult.rejectValue("roleIds", "user.roles.required", "Selecciona al menos un rol");
        }
    }

    /**
     * Enforces password length restrictions if a password value is supplied.
     */
    private void validatePasswordLength(UserManagementForm form, BindingResult bindingResult) {
        if (form.getPassword() != null && !form.getPassword().isBlank() && form.getPassword().length() < 8) {
            bindingResult.rejectValue("password", "user.password.tooShort",
                    "La contrasena debe tener al menos 8 caracteres");
        }
    }

    /**
     * Resolves role database records from their IDs.
     */
    private Set<Role> resolveRoles(Set<Long> roleIds) {
        return new HashSet<>(roles.findAllById(roleIds));
    }
}
