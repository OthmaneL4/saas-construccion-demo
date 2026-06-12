package com.lsototalbouw.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.common.enums.ToolStatus;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.material.MaterialItem;
import com.lsototalbouw.material.MaterialRepository;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.notification.BusinessNotificationService;
import com.lsototalbouw.security.SecurityRoles;
import com.lsototalbouw.tool.ToolItem;
import com.lsototalbouw.tool.ToolRepository;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.Role;
import com.lsototalbouw.user.RoleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class OperationalAlertServiceIT {

    @Autowired
    private OperationalAlertService alertService;

    @Autowired
    private BusinessNotificationService notificationService;

    @Autowired
    private BusinessNotificationRepository notifications;

    @Autowired
    private CompanyAccountRepository companies;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private MaterialRepository materials;

    @Autowired
    private ToolRepository tools;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private CompanyAccount company;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        company = companies.save(new CompanyAccount(
                "LSOTOTALBOUW", "NL001234567B01", "info@lsototalbouw.nl", "+31 6 12345678", "Rotterdam"));
        Customer customer = customers.save(new Customer(
                company, "Cliente Test", "cliente@example.nl", "+31 6 11111111", "Bouwstraat 1", "Rotterdam"));
        invoices.save(new Invoice(company, customer, null, "2026-0001",
                new BigDecimal("1200.00"), new BigDecimal("200.00"),
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(2), InvoiceStatus.SENT));
        materials.save(new MaterialItem(company, "Cemento", "sacos", 2, 5, new BigDecimal("9.50")));
        tools.save(new ToolItem(company, "Taladro profesional", "TLS-001",
                ToolStatus.IN_USE, LocalDate.now().plusDays(3)));
        createUser(company, "owner@lsototalbouw.test");
        authenticateAs("owner@lsototalbouw.test");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void currentAlertsIncludeOperationalRisksForCurrentCompany() {
        List<OperationalAlert> alerts = alertService.currentAlerts();

        assertThat(alerts).extracting(OperationalAlert::title)
                .contains("Factura vencida", "Stock bajo", "Mantenimiento cercano");
        assertThat(alerts.get(0).title()).isEqualTo("Factura vencida");
    }

    @Test
    @Transactional
    void operationalAlertsCanBePersistedAsUnreadNotifications() {
        notificationService.syncFromAlerts(alertService.currentAlerts());

        assertThat(notificationService.unreadCount()).isEqualTo(3);
        assertThat(notifications.findCurrentCompanyNotifications(company.getId()))
                .extracting(notification -> notification.getTitle())
                .contains("Factura vencida", "Stock bajo", "Mantenimiento cercano");
    }

    private void createUser(CompanyAccount company, String email) {
        Role role = roles.save(new Role("ROLE_" + SecurityRoles.OWNER + "_" + company.getId()));
        AppUser user = new AppUser(company, email, email, passwordEncoder.encode("Test123!"));
        user.getRoles().add(role);
        users.save(user);
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email,
                "n/a",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.OWNER))));
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "audit_logs", "notifications", "work_logs", "business_documents", "calendar_events", "suppliers", "tools",
                "materials", "expenses", "payments", "invoice_lines", "invoices", "quotation_lines",
                "quotations", "projects", "customers", "user_roles", "users", "roles", "company_accounts");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
