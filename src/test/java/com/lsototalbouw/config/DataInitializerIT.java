package com.lsototalbouw.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_seed_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.h2.console.enabled=false",
        "app.h2-console.enabled=false",
        "app.demo-login.enabled=true",
        "app.upload.documents-dir=target/demo-seed-it-documents"
})
@ActiveProfiles("dev")
class DataInitializerIT {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private CompanyAccountRepository companies;

    @Autowired
    private BusinessDocumentRepository documents;

    @Autowired
    private BusinessNotificationRepository notifications;

    @Test
    void devSeedCreatesDemoDocumentsAndNotificationsWithoutDuplicates() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        long initialDocuments = documents.findByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(company.getId()).size();
        long initialNotifications = notifications.findCurrentCompanyNotifications(company.getId()).size();

        assertThat(initialDocuments).isGreaterThanOrEqualTo(3);
        assertThat(initialNotifications).isGreaterThanOrEqualTo(3);
        assertThat(Files.isRegularFile(Path.of("target/demo-seed-it-documents/demo-contrato-dakrenovatie-jansen.pdf")))
                .isTrue();
        assertThat(Files.isRegularFile(Path.of("target/demo-seed-it-documents/demo-informe-reparacion-horeca-plaza.pdf")))
                .isTrue();
        assertThat(Files.isRegularFile(Path.of("target/demo-seed-it-documents/demo-fotos-gevelherstel-parkzicht.pdf")))
                .isTrue();

        dataInitializer.run();

        assertThat(documents.findByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(company.getId()))
                .hasSize((int) initialDocuments);
        assertThat(notifications.findCurrentCompanyNotifications(company.getId()))
                .hasSize((int) initialNotifications);
    }
}
