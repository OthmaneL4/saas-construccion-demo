package com.lsototalbouw.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * Data Transfer Object (DTO) capturing web form inputs for creating or updating user accounts.
 *
 * <p>Enforces text validation constraints and provides mapping hooks to transfer state
 * between the form parameters and persistent {@link AppUser} entities.
 */
public class UserManagementForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Introduce un email valido")
    @Size(max = 160)
    private String email;

    @Size(max = 120)
    private String password;

    /** Set of role identifiers assigned to the user. */
    private Set<Long> roleIds = new HashSet<>();

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds == null ? new HashSet<>() : roleIds;
    }

    /**
     * Constructs and populates a new form instance using values from an existing {@link AppUser} entity.
     *
     * @param user the source user entity
     * @return a populated {@link UserManagementForm} instance
     */
    public static UserManagementForm from(AppUser user) {
        UserManagementForm form = new UserManagementForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        user.getRoles().forEach(role -> form.getRoleIds().add(role.getId()));
        return form;
    }
}
