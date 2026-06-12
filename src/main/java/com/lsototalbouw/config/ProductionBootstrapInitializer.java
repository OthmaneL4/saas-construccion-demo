package com.lsototalbouw.config;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.security.SecurityRoles;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.Role;
import com.lsototalbouw.user.RoleRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Bootstraps the minimum production security model for a fresh database.
 *
 * <p>This initializer intentionally avoids demo data. When the production database has no users,
 * it creates the base authorization roles, the first tenant company, and a single OWNER/ADMIN account
 * from environment-backed configuration properties. If the required bootstrap credentials are missing,
 * startup fails fast so production never runs with implicit default credentials.
 */
@Component
@Profile("prod")
public class ProductionBootstrapInitializer implements CommandLineRunner {

    private static final List<String> REQUIRED_ROLES = List.of(
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.FINANCE,
            SecurityRoles.OPERATIONS,
            SecurityRoles.INVENTORY,
            SecurityRoles.DOCUMENTS,
            SecurityRoles.AUDITOR
    );

    private final CompanyAccountRepository companies;
    private final RoleRepository roles;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String companyName;
    private final String companyVatNumber;
    private final String companyEmail;
    private final String companyPhone;
    private final String companyAddress;
    private final String adminFullName;
    private final String adminEmail;
    private final String adminPassword;

    public ProductionBootstrapInitializer(CompanyAccountRepository companies, RoleRepository roles,
                                          AppUserRepository users, PasswordEncoder passwordEncoder,
                                          @Value("${app.bootstrap.company-name:LSOTOTALBOUW}") String companyName,
                                          @Value("${app.bootstrap.company-vat-number:}") String companyVatNumber,
                                          @Value("${app.bootstrap.company-email:}") String companyEmail,
                                          @Value("${app.bootstrap.company-phone:}") String companyPhone,
                                          @Value("${app.bootstrap.company-address:}") String companyAddress,
                                          @Value("${app.bootstrap.admin-full-name:LSOTOTALBOUW Owner}") String adminFullName,
                                          @Value("${app.bootstrap.admin-email:}") String adminEmail,
                                          @Value("${app.bootstrap.admin-password:}") String adminPassword) {
        this.companies = companies;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.companyName = companyName;
        this.companyVatNumber = companyVatNumber;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
        this.companyAddress = companyAddress;
        this.adminFullName = adminFullName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /**
     * Creates the production bootstrap records only when the application has no users yet.
     *
     * <p>Roles are always ensured because authorization checks depend on their stable names. The first
     * administrator is created once and never overwritten, allowing normal user management to take over
     * after the initial login.
     */
    @Override
    @Transactional
    public void run(String... args) {
        REQUIRED_ROLES.forEach(this::ensureRole);

        if (users.count() > 0) {
            return;
        }

        validateBootstrapCredentials();
        CompanyAccount company = companies.findAll().stream()
                .findFirst()
                .orElseGet(() -> companies.save(new CompanyAccount(
                        companyName,
                        blankToNull(companyVatNumber),
                        blankToNull(companyEmail),
                        blankToNull(companyPhone),
                        blankToNull(companyAddress)
                )));

        AppUser owner = new AppUser(
                company,
                adminFullName,
                adminEmail.trim().toLowerCase(),
                passwordEncoder.encode(adminPassword)
        );
        owner.getRoles().add(ensureRole(SecurityRoles.OWNER));
        owner.getRoles().add(ensureRole(SecurityRoles.ADMIN));
        users.save(owner);
    }

    private Role ensureRole(String roleName) {
        String persistedName = "ROLE_" + roleName;
        return roles.findByName(persistedName).orElseGet(() -> roles.save(new Role(persistedName)));
    }

    private void validateBootstrapCredentials() {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException(
                    "Production bootstrap requires APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD "
                            + "when the users table is empty."
            );
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters.");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
