package com.lsototalbouw.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.security.SecurityRoles;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.Role;
import com.lsototalbouw.user.RoleRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticatedNavigationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyAccountRepository companies;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        CompanyAccount company = companies.save(new CompanyAccount(
                "LSOTOTALBOUW", "NL001234567B01", "info@lsototalbouw.nl", "+31 6 12345678", "Rotterdam"));
        Role owner = roles.save(new Role("ROLE_" + SecurityRoles.OWNER));
        Role admin = roles.save(new Role("ROLE_" + SecurityRoles.ADMIN));
        AppUser user = new AppUser(company, "Smoke Test Owner", "owner@test.local",
                passwordEncoder.encode("Test123!"));
        user.getRoles().add(owner);
        user.getRoles().add(admin);
        users.save(user);
    }

    @ParameterizedTest
    @MethodSource("ownerRoutes")
    void ownerCanOpenPrimaryAuthenticatedPages(String route) throws Exception {
        mockMvc.perform(get(route).with(user("owner@test.local").roles(SecurityRoles.OWNER, SecurityRoles.ADMIN)))
                .andExpect(status().isOk());
    }

    private static Stream<String> ownerRoutes() {
        return Stream.of(
                "/dashboard",
                "/customers",
                "/projects",
                "/quotations",
                "/invoices",
                "/payments",
                "/receivables",
                "/expenses",
                "/materials",
                "/tools",
                "/suppliers",
                "/calendar",
                "/documents",
                "/work-logs",
                "/profitability",
                "/audit",
                "/users",
                "/settings/company",
                "/notifications"
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "audit_logs", "notifications", "work_logs", "business_documents", "calendar_events", "suppliers",
                "tools", "materials", "expenses", "payments", "invoice_payment_reminders", "invoice_lines",
                "invoices", "quotation_lines", "quotations", "projects", "customers", "user_roles",
                "users", "roles", "company_accounts");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
