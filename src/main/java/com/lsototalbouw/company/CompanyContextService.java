package com.lsototalbouw.company;

import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Context resolver service responsible for fetching the active tenant scope ({@link CompanyAccount}).
 *
 * <p>Retrieves the authenticated user from the active Spring Security context, resolves their associated company,
 * and falls back to the default company configuration if no user session is active.
 */
@Service
public class CompanyContextService {

    private final CompanyAccountRepository companies;
    private final AppUserRepository users;

    public CompanyContextService(CompanyAccountRepository companies, AppUserRepository users) {
        this.companies = companies;
        this.users = users;
    }

    /**
     * Resolves the {@link CompanyAccount} associated with the currently authenticated user.
     *
     * <p>If the user is not authenticated or anonymous, this method defaults to returning the first
     * configured company account found in the database.
     *
     * @return the resolved {@link CompanyAccount} instance
     * @throws IllegalStateException if the authenticated user cannot be found in the database,
     *                               or if no company account exists at all in the system
     */
    @Transactional(readOnly = true)
    public CompanyAccount currentCompany() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            AppUser user = users.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            return user.getCompanyAccount();
        }
        return companies.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No company account configured"));
     }

    /**
     * Resolves the unique identifier of the current active company.
     *
     * @return the ID of the resolved current company account
     * @throws IllegalStateException if no current company account can be resolved
     * @see #currentCompany()
     */
    @Transactional(readOnly = true)
    public Long currentCompanyId() {
        return currentCompany().getId();
    }
}
