package com.lsototalbouw.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerForm;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.customer.CustomerService;
import com.lsototalbouw.security.SecurityRoles;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.Role;
import com.lsototalbouw.user.RoleRepository;
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
class MultiCompanyIsolationIT {

    @Autowired
    private CompanyAccountRepository companies;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private CompanyAccount firstCompany;
    private CompanyAccount secondCompany;
    private Customer secondCompanyCustomer;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        firstCompany = companies.save(new CompanyAccount(
                "LSOTOTALBOUW", "NL001234567B01", "info@lsototalbouw.nl", "+31 6 12345678", "Rotterdam"));
        secondCompany = companies.save(new CompanyAccount(
                "Partner Bouw", "NL009876543B01", "info@partnerbouw.nl", "+31 6 87654321", "Amsterdam"));
        createUser(firstCompany, "owner@first.test");
        createUser(secondCompany, "owner@second.test");
        secondCompanyCustomer = customers.save(new Customer(
                secondCompany, "Cliente de otra empresa", "otro@example.nl", "+31 6 99999999", "Damrak 1", "Amsterdam"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void currentUserCannotLoadCustomerFromAnotherCompany() {
        authenticateAs("owner@first.test");

        assertThatThrownBy(() -> customerService.getCurrentCompanyCustomer(secondCompanyCustomer.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente no encontrado");
    }

    @Test
    @Transactional
    void createdCustomersAreAssignedToAuthenticatedUsersCompany() {
        authenticateAs("owner@first.test");
        Customer created = customerService.create(customerForm("Nuevo cliente", "nuevo@example.nl"));

        assertThat(created.getCompanyAccount().getId()).isEqualTo(firstCompany.getId());
        assertThat(customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(firstCompany.getId()))
                .extracting(Customer::getName)
                .contains("Nuevo cliente");
        assertThat(customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(secondCompany.getId()))
                .extracting(Customer::getName)
                .doesNotContain("Nuevo cliente");
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

    private CustomerForm customerForm(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        form.setPhone("+31 6 11111111");
        form.setAddress("Teststraat 1");
        form.setCity("Rotterdam");
        return form;
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
