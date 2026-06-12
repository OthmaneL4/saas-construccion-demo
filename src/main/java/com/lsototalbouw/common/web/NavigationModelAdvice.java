package com.lsototalbouw.common.web;

import com.lsototalbouw.security.SecurityRoles;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

/**
 * Controller advice that automatically injects navigation permissions and user attributes into all MVC models.
 *
 * <p>Ensures that the sidebar menu items are dynamically shown or hidden based on the security roles
 * of the currently authenticated user.
 */
@ControllerAdvice
public class NavigationModelAdvice {

    private final boolean publicDemoEnabled;

    public NavigationModelAdvice(@Value("${app.public-demo.enabled:false}") boolean publicDemoEnabled) {
        this.publicDemoEnabled = publicDemoEnabled;
    }

    /**
     * Intercepts controller requests to append menu access flags and the username to the view model.
     *
     * @param model          the Spring MVC {@link Model} to be populated
     * @param authentication the current security {@link Authentication} details
     */
    @ModelAttribute
    public void addNavigationPermissions(Model model, Authentication authentication) {
        Set<String> roles = roles(authentication);
        boolean fullAccess = hasAny(roles, SecurityRoles.OWNER, SecurityRoles.ADMIN);

        model.addAttribute("currentUserName", authentication == null ? "Usuario" : authentication.getName());
        model.addAttribute("canAccessAdmin", fullAccess);
        model.addAttribute("canAccessFinance", fullAccess || hasAny(roles, SecurityRoles.FINANCE));
        model.addAttribute("canAccessOperations", fullAccess || hasAny(roles, SecurityRoles.OPERATIONS));
        model.addAttribute("canAccessCustomers", fullAccess || hasAny(roles, SecurityRoles.FINANCE, SecurityRoles.OPERATIONS));
        model.addAttribute("canAccessInventory", fullAccess || hasAny(roles, SecurityRoles.INVENTORY, SecurityRoles.OPERATIONS));
        model.addAttribute("canAccessDocuments", fullAccess || hasAny(roles, SecurityRoles.DOCUMENTS, SecurityRoles.OPERATIONS));
        model.addAttribute("canAccessAudit", fullAccess || hasAny(roles, SecurityRoles.AUDITOR));
        model.addAttribute("publicDemoEnabled", publicDemoEnabled);
    }

    /**
     * Helper method to extract simplified (prefix-stripped) role names from the authentication principal.
     *
     * @param authentication the security context authentication information
     * @return a set of uppercase role strings (e.g. {@code "ADMIN", "FINANCE"}) with their "ROLE_" prefixes stripped
     */
    private Set<String> roles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Utility method to determine if the user has any of the expected security roles.
     *
     * @param roles         the set of roles possessed by the user
     * @param expectedRoles the array of role names required for the access permission
     * @return {@code true} if there is any intersection between the user's roles and the expected roles, otherwise {@code false}
     */
    private boolean hasAny(Set<String> roles, String... expectedRoles) {
        for (String expectedRole : expectedRoles) {
            if (roles.contains(expectedRole)) {
                return true;
            }
        }
        return false;
    }
}
