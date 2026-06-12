package com.lsototalbouw.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.quotation.QuotationRepository;
import com.lsototalbouw.security.SecurityRoles;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.RoleRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod_bootstrap_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.h2.console.enabled=false",
        "app.h2-console.enabled=false",
        "app.demo-login.enabled=false",
        "app.upload.documents-dir=target/prod-bootstrap-it-documents",
        "app.bootstrap.company-name=LSOTOTALBOUW",
        "app.bootstrap.company-vat-number=NL001234567B01",
        "app.bootstrap.company-email=info@lsototalbouw.nl",
        "app.bootstrap.company-phone=+31 6 12345678",
        "app.bootstrap.company-address=Rotterdam, Nederland",
        "app.bootstrap.admin-full-name=Production Bootstrap Owner",
        "app.bootstrap.admin-email=prod-owner@test.local",
        "app.bootstrap.admin-password=ProdBootstrap123!"
})
@ActiveProfiles("prod")
class ProductionBootstrapInitializerIT {

    @Autowired
    private ProductionBootstrapInitializer productionBootstrapInitializer;

    @Autowired
    private CompanyAccountRepository companies;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private QuotationRepository quotations;

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private BusinessDocumentRepository documents;

    @Autowired
    private BusinessNotificationRepository notifications;

    @Test
    @Transactional
    void productionBootstrapCreatesOnlySecurityFoundationAndIsIdempotent() throws Exception {
        assertThat(companies.findAll()).hasSize(1);
        assertThat(users.findAll()).hasSize(1);

        AppUser owner = users.findByEmailIgnoreCase("prod-owner@test.local").orElseThrow();
        assertThat(owner.getFullName()).isEqualTo("Production Bootstrap Owner");
        assertThat(owner.getCompanyAccount().getName()).isEqualTo("LSOTOTALBOUW");
        assertThat(owner.getRoles()).extracting(role -> role.getName())
                .contains("ROLE_" + SecurityRoles.OWNER, "ROLE_" + SecurityRoles.ADMIN);

        Set<String> roleNames = roles.findAll().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        assertThat(roleNames).contains(
                "ROLE_" + SecurityRoles.OWNER,
                "ROLE_" + SecurityRoles.ADMIN,
                "ROLE_" + SecurityRoles.FINANCE,
                "ROLE_" + SecurityRoles.OPERATIONS,
                "ROLE_" + SecurityRoles.INVENTORY,
                "ROLE_" + SecurityRoles.DOCUMENTS,
                "ROLE_" + SecurityRoles.AUDITOR
        );

        assertThat(customers.count()).isZero();
        assertThat(projects.count()).isZero();
        assertThat(quotations.count()).isZero();
        assertThat(invoices.count()).isZero();
        assertThat(documents.count()).isZero();
        assertThat(notifications.count()).isZero();

        productionBootstrapInitializer.run();

        assertThat(companies.findAll()).hasSize(1);
        assertThat(users.findAll()).hasSize(1);
        assertThat(roles.findAll()).hasSize(roleNames.size());
    }
}
