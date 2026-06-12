package com.lsototalbouw.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditLogRepository;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.calendar.CalendarEvent;
import com.lsototalbouw.calendar.CalendarEventRepository;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.document.BusinessDocument;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.document.DocumentCategory;
import com.lsototalbouw.expense.Expense;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceLineRepository;
import com.lsototalbouw.invoice.InvoicePaymentReminderRepository;
import com.lsototalbouw.invoice.InvoicePaymentReminderType;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.material.MaterialItem;
import com.lsototalbouw.material.MaterialRepository;
import com.lsototalbouw.notification.BusinessNotification;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.payment.Payment;
import com.lsototalbouw.payment.PaymentRepository;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.quotation.Quotation;
import com.lsototalbouw.quotation.QuotationLine;
import com.lsototalbouw.quotation.QuotationRepository;
import com.lsototalbouw.quotation.QuotationLineRepository;
import com.lsototalbouw.supplier.Supplier;
import com.lsototalbouw.supplier.SupplierRepository;
import com.lsototalbouw.timesheet.WorkLog;
import com.lsototalbouw.timesheet.WorkLogRepository;
import com.lsototalbouw.timesheet.WorkLogStatus;
import com.lsototalbouw.tool.ToolItem;
import com.lsototalbouw.tool.ToolRepository;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.Role;
import com.lsototalbouw.user.RoleRepository;
import jakarta.servlet.RequestDispatcher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "app.security.login.max-failed-attempts=3",
        "app.security.login.lock-minutes=30"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIT {

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
    private AuditLogRepository auditLogs;

    @Autowired
    private BusinessNotificationRepository notifications;

    @Autowired
    private CalendarEventRepository calendarEvents;

    @Autowired
    private BusinessDocumentRepository documents;

    @Autowired
    private ExpenseRepository expenses;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private InvoiceLineRepository invoiceLines;

    @Autowired
    private InvoicePaymentReminderRepository paymentReminders;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private WorkLogRepository workLogs;

    @Autowired
    private QuotationRepository quotations;

    @Autowired
    private QuotationLineRepository quotationLines;

    @Autowired
    private SupplierRepository suppliers;

    @Autowired
    private MaterialRepository materials;

    @Autowired
    private ToolRepository tools;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        CompanyAccount company = companies.save(new CompanyAccount(
                "LSOTOTALBOUW", "NL001234567B01", "info@lsototalbouw.nl", "+31 6 12345678", "Rotterdam"));
        createUser(company, "owner@test.local", SecurityRoles.OWNER);
        createUser(company, "finance@test.local", SecurityRoles.FINANCE);
        createUser(company, "operations@test.local", SecurityRoles.OPERATIONS);
        createUser(company, "documents@test.local", SecurityRoles.DOCUMENTS);
        createUser(company, "auditor@test.local", SecurityRoles.AUDITOR);
    }

    @Test
    void redirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void healthEndpointIsPublicForOperationalMonitoring() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    @Test
    void allowsOwnerToOpenDashboard() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Control diario de LSOTOTALBOUW")))
                .andExpect(content().string(containsString("Indicadores rapidos")));
    }

    @Test
    void dashboardShowsOnlyOpenInvoicesWithRealOutstandingAmount() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Dashboard", "dashboard@example.nl", "+31 6 10101010", "Straat 10", "Rotterdam"));
        invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-DASH-OPEN",
                new java.math.BigDecimal("500.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.now().minusDays(10),
                java.time.LocalDate.now().minusDays(1),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));
        invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-DASH-PAID",
                new java.math.BigDecimal("250.00"),
                new java.math.BigDecimal("250.00"),
                java.time.LocalDate.now().minusDays(12),
                java.time.LocalDate.now().minusDays(2),
                com.lsototalbouw.common.enums.InvoiceStatus.PAID
        ));

        mockMvc.perform(get("/dashboard").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pendiente real")))
                .andExpect(content().string(containsString("Facturas vencidas")))
                .andExpect(content().string(containsString("Vencen en 7 dias")))
                .andExpect(content().string(containsString("INV-DASH-OPEN")))
                .andExpect(content().string(containsString("EUR 400,00")))
                .andExpect(content().string(not(containsString("INV-DASH-PAID"))));
    }

    @Test
    void dashboardUsesExternalJavascriptUnderStrictContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/app.js")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("script-src 'self' 'unsafe-inline'"))));
    }

    @Test
    void staticJavascriptIsPublicSoUiEnhancementsLoadBeforeLogin() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bindPaymentAmountHelper")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    void logoutClearsConfiguredSessionCookie() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("LSOTOTALBOUWSESSION", 0));
    }

    @Test
    void ownerCanOpenUserManagementPageWithSecurityStatus() throws Exception {
        mockMvc.perform(get("/users").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuarios totales")))
                .andExpect(content().string(containsString("Activos")))
                .andExpect(content().string(containsString("Fallos recientes")))
                .andExpect(content().string(containsString("Bloqueados")))
                .andExpect(content().string(containsString("Roles asignados")))
                .andExpect(content().string(containsString("Seguridad")))
                .andExpect(content().string(containsString("OK")));
    }

    @Test
    void ownerCanOpenAndUpdateCompanySettings() throws Exception {
        mockMvc.perform(get("/settings/company").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Datos de empresa")));

        mockMvc.perform(post("/settings/company")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "LSOTOTALBOUW Updated")
                        .param("vatNumber", "NL000000001B01")
                        .param("email", "office@lsototalbouw.nl")
                        .param("phone", "+31 6 22222222")
                        .param("address", "Rotterdam, Nederland")
                        .param("kvkNumber", "12345678")
                        .param("iban", "nl91 abna 0417 1643 00")
                        .param("paymentTermsDays", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/company"));

        CompanyAccount company = companies.findAll().get(0);
        assertThat(company.getName()).isEqualTo("LSOTOTALBOUW Updated");
        assertThat(company.getKvkNumber()).isEqualTo("12345678");
        assertThat(company.getIban()).isEqualTo("NL91ABNA0417164300");
        assertThat(company.getPaymentTermsDays()).isEqualTo(30);
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.UPDATE);
    }

    @Test
    void newInvoiceUsesCompanyPaymentTermsWhenDueDateIsEmpty() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        company.updateFrom(company.getName(), company.getVatNumber(), company.getEmail(), company.getPhone(),
                company.getAddress(), "12345678", "NL91ABNA0417164300", 21);
        companies.save(company);
        Customer customer = customers.save(new Customer(
                company, "Cliente Factura", "factura@example.nl", "+31 6 11111111", "Straat 1", "Rotterdam"));

        mockMvc.perform(post("/invoices")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("customerId", customer.getId().toString())
                        .param("invoiceNumber", "INV-TERMS-001")
                        .param("amount", "100.00")
                        .param("paidAmount", "0.00")
                        .param("issueDate", "2026-05-29")
                        .param("status", "SENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices"));

        Invoice invoice = invoices.findAll().stream()
                .filter(item -> "INV-TERMS-001".equals(item.getInvoiceNumber()))
                .findFirst()
                .orElseThrow();
        assertThat(invoice.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 6, 19));
    }

    @Test
    void invoiceAndQuotationFormsShowAutomaticBusinessNumbers() throws Exception {
        mockMvc.perform(get("/invoices").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-" + java.time.LocalDate.now().getYear() + "-0001")))
                .andExpect(content().string(containsString("Propuesto automaticamente")))
                .andExpect(content().string(containsString("Enviada")))
                .andExpect(content().string(containsString("Parcialmente pagada")));

        mockMvc.perform(get("/projects").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planificado")))
                .andExpect(content().string(containsString("En curso")));

        mockMvc.perform(get("/quotations").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OFF-" + java.time.LocalDate.now().getYear() + "-0001")))
                .andExpect(content().string(containsString("Propuesto automaticamente")))
                .andExpect(content().string(containsString("Enviado")))
                .andExpect(content().string(containsString("Aceptado")));
    }

    @Test
    void ownerCanFilterInvoicesByDueDateAndSeeFilteredTotals() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Filtro Facturas", "filtro-facturas@example.nl", "+31 6 11112222", "Straat 3", "Rotterdam"));
        invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-FILTER-001",
                new java.math.BigDecimal("1000.00"),
                new java.math.BigDecimal("250.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 20),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));
        invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-FILTER-OLD",
                new java.math.BigDecimal("300.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 20),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(get("/invoices")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "FILTER")
                        .param("status", "SENT")
                        .param("dueFrom", "2026-06-01")
                        .param("dueTo", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Facturas filtradas")))
                .andExpect(content().string(containsString("Total filtrado")))
                .andExpect(content().string(containsString("Pendiente filtrado")))
                .andExpect(content().string(containsString("INV-FILTER-001")))
                .andExpect(content().string(not(containsString("INV-FILTER-OLD"))))
                .andExpect(content().string(containsString("EUR 1.000,00")))
                .andExpect(content().string(containsString("EUR 250,00")))
                .andExpect(content().string(containsString("EUR 750,00")));
    }

    @Test
    void ownerCanFilterQuotationsByStatusSearchAndValidity() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Filtro Presupuestos", "filtro-presupuestos@example.nl", "+31 6 13131313", "Straat 13", "Rotterdam"));
        quotations.save(new Quotation(
                company,
                customer,
                "OFF-FILTER-001",
                "Reforma bano premium",
                "Trabajos aceptados",
                new java.math.BigDecimal("1800.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.QuotationStatus.ACCEPTED
        ));
        quotations.save(new Quotation(
                company,
                customer,
                "OFF-FILTER-OLD",
                "Reforma bano pendiente",
                "Fuera del periodo",
                new java.math.BigDecimal("700.00"),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 30),
                com.lsototalbouw.common.enums.QuotationStatus.SENT
        ));

        mockMvc.perform(get("/quotations")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "bano")
                        .param("customerId", customer.getId().toString())
                        .param("status", "ACCEPTED")
                        .param("validFrom", "2026-06-01")
                        .param("validTo", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Presupuestos filtrados")))
                .andExpect(content().string(containsString("Total presupuestado")))
                .andExpect(content().string(containsString("Importe aceptado")))
                .andExpect(content().string(containsString("OFF-FILTER-001")))
                .andExpect(content().string(not(containsString("OFF-FILTER-OLD"))))
                .andExpect(content().string(containsString("EUR 1.800,00")))
                .andExpect(content().string(containsString("status-ACCEPTED")))
                .andExpect(content().string(containsString("value=\"2026-06-01\"")))
                .andExpect(content().string(containsString("value=\"2026-06-30\"")));
    }

    @Test
    void ownerCanConvertQuotationToInvoiceWithCopiedLines() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Conversion", "conversion@example.nl", "+31 6 33333333", "Straat 2", "Rotterdam"));
        Quotation quotation = quotations.save(new Quotation(
                company,
                customer,
                "OFF-2026-0099",
                "Reforma cocina",
                "Trabajos aceptados",
                new java.math.BigDecimal("121.00"),
                java.time.LocalDate.of(2026, 5, 30),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.QuotationStatus.SENT
        ));
        quotationLines.save(new QuotationLine(
                quotation,
                "Mano de obra",
                new java.math.BigDecimal("1.00"),
                new java.math.BigDecimal("100.00"),
                new java.math.BigDecimal("21.00")
        ));

        mockMvc.perform(post("/quotations/{id}/convert-to-invoice", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/invoices/*"));

        Invoice invoice = invoices.findAll().stream()
                .filter(item -> item.getCustomer().getId().equals(customer.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(invoice.getInvoiceNumber()).startsWith("INV-");
        assertThat(invoice.getQuotation()).isNotNull();
        assertThat(invoice.getQuotation().getId()).isEqualTo(quotation.getId());
        assertThat(invoice.getAmount()).isEqualByComparingTo("121.00");
        assertThat(invoiceLines.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(
                company.getId(), invoice.getId())).hasSize(1);
        assertThat(quotations.findById(quotation.getId()).orElseThrow().getStatus())
                .isEqualTo(com.lsototalbouw.common.enums.QuotationStatus.ACCEPTED);
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CONVERT);
    }

    @Test
    void ownerCanUpdateQuotationStatusFromDetailAction() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Estado", "estado@example.nl", "+31 6 22223333", "Straat 8", "Rotterdam"));
        Quotation quotation = quotations.save(new Quotation(
                company,
                customer,
                "OFF-2026-0110",
                "Pintura interior",
                "Cambio rapido de estado",
                new java.math.BigDecimal("350.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 7, 1),
                com.lsototalbouw.common.enums.QuotationStatus.DRAFT
        ));

        mockMvc.perform(get("/quotations/{id}", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aceptar")))
                .andExpect(content().string(containsString("Rechazar")));

        mockMvc.perform(post("/quotations/{id}/status", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("status", "SENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quotations/" + quotation.getId()));

        assertThat(quotations.findById(quotation.getId()).orElseThrow().getStatus())
                .isEqualTo(com.lsototalbouw.common.enums.QuotationStatus.SENT);
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.UPDATE);
    }

    @Test
    void ownerCanRegisterInvoicePaymentReminder() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Reclamacion", "reclamacion@example.nl", "+31 6 55555555", "Straat 4", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-REM-001",
                new java.math.BigDecimal("300.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 10),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(post("/invoices/{id}/payment-reminder", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("template", "SECOND_NOTICE")
                        .param("message", "Cliente avisado por telefono. Pago previsto esta semana."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + invoice.getId()));

        Invoice updated = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getPaymentReminderCount()).isEqualTo(1);
        assertThat(updated.getLastPaymentReminderAt()).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.OVERDUE);
        assertThat(paymentReminders.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderBySentAtDesc(
                company.getId(), invoice.getId()))
                .hasSize(1)
                .first()
                .satisfies(reminder -> {
                    assertThat(reminder.getReminderType()).isEqualTo(InvoicePaymentReminderType.MANUAL);
                    assertThat(reminder.getOutstandingAmount()).isEqualByComparingTo("300.00");
                    assertThat(reminder.getGeneratedByEmail()).isEqualTo("owner@test.local");
                    assertThat(reminder.getMessage()).contains("Segundo aviso");
                    assertThat(reminder.getMessage()).contains("Cliente avisado por telefono");
                });
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.REMINDER_SENT);

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reclamaciones")))
                .andExpect(content().string(containsString("Ultima reclamacion")))
                .andExpect(content().string(containsString("Historial de reclamaciones")))
                .andExpect(content().string(containsString("Primer aviso")))
                .andExpect(content().string(containsString("Segundo aviso")))
                .andExpect(content().string(containsString("Aviso final")))
                .andExpect(content().string(containsString("Cliente avisado por telefono")))
                .andExpect(content().string(containsString("Manual")))
                .andExpect(content().string(containsString("Registrada")))
                .andExpect(content().string(not(containsString(">MANUAL<"))))
                .andExpect(content().string(not(containsString(">REGISTERED<"))));
    }

    @Test
    void ownerCanRegisterPaymentFromInvoiceContext() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Pago Contextual", "pago-contextual@example.nl", "+31 6 10101010", "Straat 10", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-PAY-CTX-001",
                new java.math.BigDecimal("500.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrar pago")))
                .andExpect(content().string(containsString("Pagos de la factura")))
                .andExpect(content().string(containsString("EUR 400,00")));

        mockMvc.perform(get("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("invoiceId", invoice.getId().toString())
                        .param("returnUrl", "/invoices/" + invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-PAY-CTX-001")))
                .andExpect(content().string(containsString("data-outstanding=\"400.00\"")))
                .andExpect(content().string(containsString("400.00")))
                .andExpect(content().string(containsString("/invoices/" + invoice.getId())));

        mockMvc.perform(post("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("invoiceId", invoice.getId().toString())
                        .param("amount", "400.00")
                        .param("paymentDate", "2026-06-05")
                        .param("method", "Transferencia")
                        .param("status", "COMPLETED")
                        .param("returnUrl", "/invoices/" + invoice.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + invoice.getId()));

        Invoice updated = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getPaidAmount()).isEqualByComparingTo("500.00");
        assertThat(updated.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PAID);
        assertThat(payments.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(
                company.getId(), invoice.getId())).hasSize(1);
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE);
    }

    @Test
    void unsafePaymentReturnUrlIsIgnored() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Redirect Pago", "redirect-pago@example.nl", "+31 6 12121212", "Straat 12", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-REDIRECT-PAY-001",
                new java.math.BigDecimal("120.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(get("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("invoiceId", invoice.getId().toString())
                        .param("returnUrl", "https://evil.example/steal"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("https://evil.example/steal"))));

        mockMvc.perform(post("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("invoiceId", invoice.getId().toString())
                        .param("amount", "120.00")
                        .param("paymentDate", "2026-06-05")
                        .param("method", "Transferencia")
                        .param("status", "COMPLETED")
                        .param("returnUrl", "https://evil.example/steal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments"));
    }

    @Test
    void completedPaymentCannotExceedInvoiceOutstandingAmount() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Sobrepago", "sobrepago@example.nl", "+31 6 20202020", "Straat 20", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-OVERPAY-001",
                new java.math.BigDecimal("300.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));

        mockMvc.perform(post("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("invoiceId", invoice.getId().toString())
                        .param("amount", "250.00")
                        .param("paymentDate", "2026-06-05")
                        .param("method", "Transferencia")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El importe no puede superar el pendiente de la factura")));

        Invoice unchanged = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(unchanged.getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(unchanged.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID);
        assertThat(payments.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(
                company.getId(), invoice.getId())).isEmpty();
    }

    @Test
    void paymentEditShowsAvailableAmountIncludingCurrentCompletedPayment() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Edita Pago", "edita-pago@example.nl", "+31 6 21212121", "Straat 21", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-PAY-EDIT-001",
                new java.math.BigDecimal("500.00"),
                new java.math.BigDecimal("200.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));
        Payment payment = payments.save(new Payment(
                company,
                invoice,
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.of(2026, 6, 8),
                "Transferencia",
                com.lsototalbouw.common.enums.PaymentStatus.COMPLETED
        ));

        mockMvc.perform(get("/payments/{id}/edit", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-PAY-EDIT-001")))
                .andExpect(content().string(containsString("data-outstanding=\"400.00\"")))
                .andExpect(content().string(containsString("Disponible para esta factura")));
    }

    @Test
    void ownerCanConfirmPendingPaymentFromDetail() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Confirma Pago", "confirma-pago@example.nl", "+31 6 30303030", "Straat 30", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-CONFIRM-001",
                new java.math.BigDecimal("300.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));
        Payment payment = payments.save(new Payment(
                company,
                invoice,
                new java.math.BigDecimal("200.00"),
                java.time.LocalDate.of(2026, 6, 8),
                "Transferencia",
                com.lsototalbouw.common.enums.PaymentStatus.PENDING
        ));

        mockMvc.perform(get("/payments/{id}", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar pago")))
                .andExpect(content().string(containsString("status-PENDING")))
                .andExpect(content().string(containsString("Pendiente")));

        mockMvc.perform(post("/payments/{id}/confirm", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/" + payment.getId()));

        Invoice updatedInvoice = invoices.findById(invoice.getId()).orElseThrow();
        Payment updatedPayment = payments.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(com.lsototalbouw.common.enums.PaymentStatus.COMPLETED);
        assertThat(updatedInvoice.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(updatedInvoice.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PAID);
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.UPDATE);
    }

    @Test
    void ownerCanConfirmPendingPaymentFromInvoiceDetail() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Confirma Desde Factura", "confirma-factura@example.nl", "+31 6 40404040", "Straat 40", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-CONFIRM-INVOICE-001",
                new java.math.BigDecimal("500.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));
        Payment payment = payments.save(new Payment(
                company,
                invoice,
                new java.math.BigDecimal("400.00"),
                java.time.LocalDate.of(2026, 6, 9),
                "Transferencia",
                com.lsototalbouw.common.enums.PaymentStatus.PENDING
        ));

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/payments/" + payment.getId() + "/confirm")))
                .andExpect(content().string(containsString("returnUrl=/invoices/" + invoice.getId())));

        mockMvc.perform(post("/payments/{id}/confirm", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("returnUrl", "/invoices/" + invoice.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + invoice.getId()));

        Invoice updatedInvoice = invoices.findById(invoice.getId()).orElseThrow();
        Payment updatedPayment = payments.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(com.lsototalbouw.common.enums.PaymentStatus.COMPLETED);
        assertThat(updatedInvoice.getPaidAmount()).isEqualByComparingTo("500.00");
        assertThat(updatedInvoice.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PAID);
    }

    @Test
    void paymentsPageShowsOperationalSummary() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Resumen Pagos", "resumen-pagos@example.nl", "+31 6 50505050", "Straat 50", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-PAY-SUMMARY-001",
                new java.math.BigDecimal("700.00"),
                new java.math.BigDecimal("250.00"),
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.PARTIALLY_PAID
        ));
        payments.save(new Payment(
                company,
                invoice,
                new java.math.BigDecimal("250.00"),
                java.time.LocalDate.of(2026, 6, 5),
                "Transferencia",
                com.lsototalbouw.common.enums.PaymentStatus.COMPLETED
        ));
        Payment pendingPayment = payments.save(new Payment(
                company,
                invoice,
                new java.math.BigDecimal("450.00"),
                java.time.LocalDate.of(2026, 6, 9),
                "Transferencia",
                com.lsototalbouw.common.enums.PaymentStatus.PENDING
        ));

        mockMvc.perform(get("/payments").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagos visibles")))
                .andExpect(content().string(containsString("Confirmado")))
                .andExpect(content().string(containsString("paymentFromFilter")))
                .andExpect(content().string(containsString("paymentToFilter")))
                .andExpect(content().string(containsString("EUR 250,00")))
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(containsString("EUR 450,00")))
                .andExpect(content().string(containsString("Pagos pendientes")))
                .andExpect(content().string(containsString("Requieren seguimiento")))
                .andExpect(content().string(containsString("status-COMPLETED")))
                .andExpect(content().string(containsString("status-PENDING")))
                .andExpect(content().string(containsString("Confirmado")))
                .andExpect(content().string(containsString("Pendiente")));

        mockMvc.perform(get("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "resumen")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-PAY-SUMMARY-001")))
                .andExpect(content().string(containsString("EUR 450,00")))
                .andExpect(content().string(containsString("Pagos pendientes")))
                .andExpect(content().string(containsString("name=\"returnUrl\" value=\"/payments?status=PENDING&amp;q=resumen\"")))
                .andExpect(content().string(not(containsString("EUR 250,00"))))
                .andExpect(content().string(not(containsString("status-COMPLETED"))));

        mockMvc.perform(post("/payments/{id}/confirm", pendingPayment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("returnUrl", "/payments?status=PENDING&q=resumen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments?status=PENDING&q=resumen"));

        mockMvc.perform(get("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("from", "2026-06-09")
                        .param("to", "2026-06-09"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-PAY-SUMMARY-001")))
                .andExpect(content().string(containsString("EUR 450,00")))
                .andExpect(content().string(containsString("value=\"2026-06-09\"")))
                .andExpect(content().string(not(containsString("EUR 250,00"))));
    }

    @Test
    void paymentLifecycleWritesAuditableFinancialTrace() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Auditoria Pagos", "audit-payments@example.nl", "+31 6 60606060", "Straat 60", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-AUDIT-PAY-001",
                new java.math.BigDecimal("600.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(post("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("invoiceId", invoice.getId().toString())
                        .param("amount", "300.00")
                        .param("paymentDate", "2026-06-10")
                        .param("method", "Transferencia")
                        .param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments"));

        Payment payment = payments.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByPaymentDateDesc(
                        company.getId(), invoice.getId())
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/payments/{id}/confirm", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/" + payment.getId()));

        mockMvc.perform(post("/payments/{id}/archive", payment.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments"));

        List<com.lsototalbouw.audit.AuditLog> paymentLogs = auditLogs
                .findTop100ByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(company.getId())
                .stream()
                .filter(log -> "Pagos".equals(log.getModuleName()))
                .filter(log -> payment.getId().toString().equals(log.getEntityId()))
                .toList();

        assertThat(paymentLogs)
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.ARCHIVE);
        assertThat(paymentLogs)
                .allSatisfy(log -> {
                    assertThat(log.getUser()).isNotNull();
                    assertThat(log.getUser().getEmail()).isEqualTo("owner@test.local");
                    assertThat(log.getSummary()).contains("Pago");
                });
    }

    @Test
    void ownerCanGeneratePaymentReminderPdf() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Carta", "carta@example.nl", "+31 6 88888888", "Straat 7", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-LETTER-001",
                new java.math.BigDecimal("450.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 10),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        byte[] body = mockMvc.perform(post("/invoices/{id}/payment-reminder/pdf", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("template", "FINAL_NOTICE")
                        .param("message", "Adjuntamos esta carta para formalizar el seguimiento del cobro."))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        Invoice updated = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getPaymentReminderCount()).isEqualTo(1);
        assertThat(updated.getLastPaymentReminderAt()).isNotNull();
        assertThat(paymentReminders.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderBySentAtDesc(
                company.getId(), invoice.getId()))
                .hasSize(1)
                .first()
                .satisfies(reminder -> {
                    assertThat(reminder.getReminderType()).isEqualTo(InvoicePaymentReminderType.PDF);
                    assertThat(reminder.getOutstandingAmount()).isEqualByComparingTo("450.00");
                    assertThat(reminder.getMessage()).contains("Aviso final");
                    assertThat(reminder.getMessage()).contains("Adjuntamos esta carta");
                });
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.REMINDER_SENT, AuditAction.PDF_GENERATE);
    }

    @Test
    void missingBusinessRecordShowsFriendlyErrorPage() throws Exception {
        mockMvc.perform(get("/invoices/{id}", 99999L)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No hemos encontrado ese registro")))
                .andExpect(content().string(containsString("Factura no encontrada")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Whitelabel"))));
    }

    @Test
    void financeDashboardShowsPrioritizedReceivables() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Cobros", "cobros@example.nl", "+31 6 77777777", "Straat 6", "Rotterdam"));
        Customer futureCustomer = customers.save(new Customer(
                company, "Cliente Futuro", "futuro@example.nl", "+31 6 77777778", "Straat 7", "Rotterdam"));
        invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-COLLECT-001",
                new java.math.BigDecimal("500.00"),
                new java.math.BigDecimal("100.00"),
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now().minusDays(20),
                com.lsototalbouw.common.enums.InvoiceStatus.OVERDUE
        ));
        invoices.save(new Invoice(
                company,
                futureCustomer,
                null,
                "INV-FUTURE-001",
                new java.math.BigDecimal("250.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusDays(30),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(get("/receivables").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cobros pendientes")))
                .andExpect(content().string(containsString("Prioridad de cobro")))
                .andExpect(content().string(containsString("receivableSearch")))
                .andExpect(content().string(containsString("Max dias")))
                .andExpect(content().string(containsString("INV-COLLECT-001")))
                .andExpect(content().string(containsString("Cliente Cobros")))
                .andExpect(content().string(containsString("Alta")));

        mockMvc.perform(get("/receivables")
                        .with(user("finance@test.local").roles(SecurityRoles.FINANCE))
                        .param("q", "Cobros")
                        .param("priority", "Alta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-COLLECT-001")))
                .andExpect(content().string(containsString("Cliente Cobros")))
                .andExpect(content().string(not(containsString("INV-FUTURE-001"))));

        mockMvc.perform(get("/receivables")
                        .with(user("finance@test.local").roles(SecurityRoles.FINANCE))
                        .param("priority", "Normal"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-FUTURE-001")))
                .andExpect(content().string(containsString("Cliente Futuro")))
                .andExpect(content().string(not(containsString("INV-COLLECT-001"))));

        mockMvc.perform(get("/receivables")
                        .with(user("finance@test.local").roles(SecurityRoles.FINANCE))
                        .param("priority", "Prioridad manipulada"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-COLLECT-001")))
                .andExpect(content().string(containsString("INV-FUTURE-001")));
    }

    @Test
    void ownerCanRegisterPaymentFromReceivablesContext() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Cobro Rapido", "cobro-rapido@example.nl", "+31 6 61616161", "Straat 61", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-FAST-COLLECT-001",
                new java.math.BigDecimal("350.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 20),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT
        ));

        mockMvc.perform(get("/receivables").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-FAST-COLLECT-001")))
                .andExpect(content().string(containsString("/payments?invoiceId=" + invoice.getId() + "&amp;returnUrl=/receivables")));

        mockMvc.perform(get("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("invoiceId", invoice.getId().toString())
                        .param("returnUrl", "/receivables"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-FAST-COLLECT-001")))
                .andExpect(content().string(containsString("350.00")))
                .andExpect(content().string(containsString("/receivables")));

        mockMvc.perform(post("/payments")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("invoiceId", invoice.getId().toString())
                        .param("amount", "350.00")
                        .param("paymentDate", "2026-06-10")
                        .param("method", "Transferencia")
                        .param("status", "COMPLETED")
                        .param("returnUrl", "/receivables"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receivables"));

        Invoice updated = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getPaidAmount()).isEqualByComparingTo("350.00");
        assertThat(updated.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PAID);
    }

    @Test
    void paidInvoiceCannotBeMarkedAsPaymentReminderSent() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Pagado", "pagado@example.nl", "+31 6 66666666", "Straat 5", "Rotterdam"));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                null,
                "INV-PAID-001",
                new java.math.BigDecimal("120.00"),
                new java.math.BigDecimal("120.00"),
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 10),
                com.lsototalbouw.common.enums.InvoiceStatus.PAID
        ));

        mockMvc.perform(post("/invoices/{id}/payment-reminder", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + invoice.getId()));

        Invoice updated = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getPaymentReminderCount()).isZero();
        assertThat(updated.getLastPaymentReminderAt()).isNull();
        assertThat(updated.getStatus()).isEqualTo(com.lsototalbouw.common.enums.InvoiceStatus.PAID);
    }

    @Test
    void ownerCannotConvertSameQuotationTwice() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Duplicado", "duplicado@example.nl", "+31 6 44444444", "Straat 3", "Rotterdam"));
        Quotation quotation = quotations.save(new Quotation(
                company,
                customer,
                "OFF-2026-0100",
                "Tejado",
                "Cambio de cubierta",
                new java.math.BigDecimal("242.00"),
                java.time.LocalDate.of(2026, 5, 30),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.QuotationStatus.SENT
        ));
        quotationLines.save(new QuotationLine(
                quotation,
                "Materiales",
                new java.math.BigDecimal("2.00"),
                new java.math.BigDecimal("100.00"),
                new java.math.BigDecimal("21.00")
        ));

        mockMvc.perform(post("/quotations/{id}/convert-to-invoice", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/invoices/*"));

        mockMvc.perform(post("/quotations/{id}/convert-to-invoice", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quotations/" + quotation.getId()));

        assertThat(invoices.findAll().stream()
                .filter(invoice -> invoice.getQuotation() != null
                        && invoice.getQuotation().getId().equals(quotation.getId()))
                .count()).isEqualTo(1);

        mockMvc.perform(get("/quotations/{id}", quotation.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver factura")));
    }

    @Test
    void deniesFinanceRoleFromUserManagementAndProjects() throws Exception {
        mockMvc.perform(get("/users").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/projects").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/settings/company").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void forbiddenErrorPageIsProfessional() throws Exception {
        mockMvc.perform(get("/error/403").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("No tienes permisos para acceder")));
    }

    @Test
    void sidebarOnlyShowsAllowedModulesForFinanceRole() throws Exception {
        mockMvc.perform(get("/invoices").with(user("finance@test.local").roles(SecurityRoles.FINANCE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/invoices\"")))
                .andExpect(content().string(containsString("href=\"/payments\"")))
                .andExpect(content().string(not(containsString("href=\"/projects\""))))
                .andExpect(content().string(not(containsString("href=\"/materials\""))))
                .andExpect(content().string(not(containsString("href=\"/audit\""))))
                .andExpect(content().string(not(containsString("href=\"/users\""))));
    }

    @Test
    void sidebarOnlyShowsAllowedModulesForAuditorRole() throws Exception {
        mockMvc.perform(get("/audit").with(user("auditor@test.local").roles(SecurityRoles.AUDITOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/audit\"")))
                .andExpect(content().string(containsString("href=\"/notifications\"")))
                .andExpect(content().string(not(containsString("href=\"/invoices\""))))
                .andExpect(content().string(not(containsString("href=\"/projects\""))))
                .andExpect(content().string(not(containsString("href=\"/materials\""))))
                .andExpect(content().string(not(containsString("href=\"/users\""))));
    }

    @Test
    void documentsRoleCanOnlyAccessDocumentWorkspaceAndSharedAppAreas() throws Exception {
        mockMvc.perform(get("/documents").with(user("documents@test.local").roles(SecurityRoles.DOCUMENTS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/documents\"")))
                .andExpect(content().string(containsString("href=\"/notifications\"")))
                .andExpect(content().string(not(containsString("href=\"/invoices\""))))
                .andExpect(content().string(not(containsString("href=\"/projects\""))))
                .andExpect(content().string(not(containsString("href=\"/materials\""))))
                .andExpect(content().string(not(containsString("href=\"/audit\""))))
                .andExpect(content().string(not(containsString("href=\"/users\""))));

        mockMvc.perform(get("/invoices").with(user("documents@test.local").roles(SecurityRoles.DOCUMENTS)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit").with(user("documents@test.local").roles(SecurityRoles.DOCUMENTS)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users").with(user("documents@test.local").roles(SecurityRoles.DOCUMENTS)))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCanFilterAuditTrailByActionModuleAndUser() throws Exception {
        String auditDate = java.time.LocalDate.now().toString();
        String outsideDate = java.time.LocalDate.now().plusDays(1).toString();

        mockMvc.perform(post("/customers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Cliente Filtro Auditor")
                        .param("email", "filtro-auditor@example.nl")
                        .param("phone", "+31 6 98989898")
                        .param("address", "Auditstraat 1")
                        .param("city", "Rotterdam"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/expenses")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("description", "Gasto fuera de filtro")
                        .param("amount", "25.00")
                        .param("category", "Varios")
                        .param("expenseDate", auditDate))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/audit")
                        .with(user("auditor@test.local").roles(SecurityRoles.AUDITOR))
                        .param("action", "CREATE")
                        .param("module", "Clientes")
                        .param("user", "owner@test.local")
                        .param("from", auditDate)
                        .param("to", auditDate))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente creado: Cliente Filtro Auditor")))
                .andExpect(content().string(containsString("owner@test.local")))
                .andExpect(content().string(containsString("Clientes")))
                .andExpect(content().string(containsString("Eventos filtrados")))
                .andExpect(content().string(containsString("Cambios de datos")))
                .andExpect(content().string(containsString("Eventos de login")))
                .andExpect(content().string(containsString("Eventos criticos")))
                .andExpect(content().string(containsString("value=\"" + auditDate + "\"")))
                .andExpect(content().string(not(containsString("Gasto fuera de filtro"))));

        mockMvc.perform(get("/audit/export")
                        .with(user("auditor@test.local").roles(SecurityRoles.AUDITOR))
                        .param("action", "CREATE")
                        .param("module", "Clientes")
                        .param("user", "owner@test.local")
                        .param("from", auditDate)
                        .param("to", auditDate))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("audit-log.csv")))
                .andExpect(content().string(containsString("Cliente creado: Cliente Filtro Auditor")))
                .andExpect(content().string(containsString("owner@test.local")))
                .andExpect(content().string(not(containsString("Gasto fuera de filtro"))));

        mockMvc.perform(get("/audit")
                        .with(user("auditor@test.local").roles(SecurityRoles.AUDITOR))
                        .param("action", "CREATE")
                        .param("module", "Clientes")
                        .param("user", "owner@test.local")
                        .param("from", outsideDate)
                        .param("to", outsideDate))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Cliente creado: Cliente Filtro Auditor"))));
    }

    @Test
    void authenticatedUnknownPageShowsProfessionalNotFoundPage() throws Exception {
        mockMvc.perform(get("/zona-inexistente").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/error")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Pagina no encontrada")))
                .andExpect(content().string(containsString("La pagina o el registro solicitado no existe")));
    }

    @Test
    void deniesOperationsRoleFromFinanceModules() throws Exception {
        mockMvc.perform(get("/invoices").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/receivables").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/profitability").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanFilterProfitabilityByProjectCustomerAndHealth() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer profitableCustomer = customers.save(new Customer(
                company, "Cliente Margen Alto", "margen-alto@example.nl", "+31 6 30000001", "Straat 30", "Rotterdam"));
        Customer riskCustomer = customers.save(new Customer(
                company, "Cliente Riesgo", "riesgo@example.nl", "+31 6 30000002", "Straat 31", "Utrecht"));
        com.lsototalbouw.project.Project profitableProject = projects.save(new com.lsototalbouw.project.Project(
                company,
                profitableCustomer,
                "Reforma margen alto",
                "Proyecto con margen positivo",
                java.time.LocalDate.of(2026, 6, 1),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("3000.00")));
        com.lsototalbouw.project.Project riskProject = projects.save(new com.lsototalbouw.project.Project(
                company,
                riskCustomer,
                "Obra con riesgo",
                "Proyecto con costes por encima de ingresos",
                java.time.LocalDate.of(2026, 6, 2),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("1200.00")));
        invoices.save(new Invoice(
                company,
                profitableCustomer,
                profitableProject,
                "PROFIT-001",
                new java.math.BigDecimal("1800.00"),
                new java.math.BigDecimal("1800.00"),
                java.time.LocalDate.of(2026, 6, 3),
                java.time.LocalDate.of(2026, 6, 17),
                com.lsototalbouw.common.enums.InvoiceStatus.PAID));
        expenses.save(new Expense(
                company,
                riskProject,
                "Materiales con desviacion",
                new java.math.BigDecimal("900.00"),
                "Materiales",
                java.time.LocalDate.of(2026, 6, 4)));

        mockMvc.perform(get("/profitability")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "margen")
                        .param("health", "Rentable"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reforma margen alto")))
                .andExpect(content().string(containsString("Cliente Margen Alto")))
                .andExpect(content().string(containsString("Rentable")))
                .andExpect(content().string(not(containsString("Obra con riesgo"))));

        mockMvc.perform(get("/profitability")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("health", "Riesgo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Obra con riesgo")))
                .andExpect(content().string(containsString("Riesgo")))
                .andExpect(content().string(not(containsString("Reforma margen alto"))));
    }

    @Test
    void toolsPageShowsBusinessStatusLabels() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        tools.save(new ToolItem(
                company,
                "Taladro profesional",
                "TLS-001",
                com.lsototalbouw.common.enums.ToolStatus.AVAILABLE,
                java.time.LocalDate.of(2026, 7, 1)));
        tools.save(new ToolItem(
                company,
                "Lijadora orbital",
                "TLS-003",
                com.lsototalbouw.common.enums.ToolStatus.IN_USE,
                java.time.LocalDate.of(2026, 7, 15)));
        tools.save(new ToolItem(
                company,
                "Sierra circular",
                "TLS-002",
                com.lsototalbouw.common.enums.ToolStatus.MAINTENANCE,
                java.time.LocalDate.of(2026, 6, 1)));
        tools.save(new ToolItem(
                company,
                "Martillo retirado",
                "TLS-004",
                com.lsototalbouw.common.enums.ToolStatus.RETIRED,
                null));

        mockMvc.perform(get("/tools").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Herramientas filtradas")))
                .andExpect(content().string(containsString("Disponibles")))
                .andExpect(content().string(containsString("En uso")))
                .andExpect(content().string(containsString("Mantenimiento vencido")))
                .andExpect(content().string(containsString("No operativas")))
                .andExpect(content().string(containsString("Taladro profesional")))
                .andExpect(content().string(containsString("Disponible")))
                .andExpect(content().string(containsString("Martillo retirado")))
                .andExpect(content().string(containsString("Retirada")))
                .andExpect(content().string(not(containsString(">AVAILABLE<"))));

        mockMvc.perform(get("/tools")
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .param("status", "MAINTENANCE")
                        .param("maintenanceDue", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sierra circular")))
                .andExpect(content().string(containsString("En mantenimiento")))
                .andExpect(content().string(containsString("Mantenimiento vencido")))
                .andExpect(content().string(not(containsString("Taladro profesional"))))
                .andExpect(content().string(not(containsString("Lijadora orbital"))));
    }

    @Test
    void materialsPageShowsInventorySummaryAndLowStockFilter() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        materials.save(new MaterialItem(
                company,
                "Madera estructural",
                "ud",
                2,
                5,
                new java.math.BigDecimal("12.50")));
        materials.save(new MaterialItem(
                company,
                "Tornillos inox",
                "caja",
                20,
                5,
                new java.math.BigDecimal("3.00")));

        mockMvc.perform(get("/materials").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Materiales filtrados")))
                .andExpect(content().string(containsString("Valor en almacen")))
                .andExpect(content().string(containsString("Reposicion minima")))
                .andExpect(content().string(containsString("EUR 85,00")))
                .andExpect(content().string(containsString("Madera estructural")))
                .andExpect(content().string(containsString("Tornillos inox")));

        mockMvc.perform(get("/materials")
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Madera estructural")))
                .andExpect(content().string(containsString("EUR 25,00")))
                .andExpect(content().string(containsString("EUR 37,50")))
                .andExpect(content().string(not(containsString("Tornillos inox"))));
    }

    @Test
    void ownerCanManageSuppliersWithAuditTrail() throws Exception {
        mockMvc.perform(get("/suppliers").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo proveedor")))
                .andExpect(content().string(containsString("Proveedores activos")))
                .andExpect(content().string(containsString("Con email")))
                .andExpect(content().string(containsString("Telefono")))
                .andExpect(content().string(containsString("Direccion")));

        mockMvc.perform(post("/suppliers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Bouwmaterialen Rotterdam")
                        .param("contactName", "Samir")
                        .param("email", "ventas@bouwmaterialen.example")
                        .param("phone", "+31 6 90909090")
                        .param("address", "Havenstraat 40")
                        .param("city", "Rotterdam"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers"));

        Supplier supplier = suppliers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companies.findAll().get(0).getId())
                .stream()
                .filter(item -> "Bouwmaterialen Rotterdam".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        suppliers.save(new Supplier(
                companies.findAll().get(0),
                "Dakservice Utrecht",
                "Nadia",
                "info@dakservice.example",
                "+31 6 80808080",
                "Dakstraat 8",
                "Utrecht"));

        mockMvc.perform(get("/suppliers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "Samir"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bouwmaterialen Rotterdam")))
                .andExpect(content().string(containsString("1 proveedores")))
                .andExpect(content().string(not(containsString("Dakservice Utrecht"))));

        mockMvc.perform(post("/suppliers/{id}/edit", supplier.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Bouwmaterialen Rotterdam BV")
                        .param("contactName", "Samir")
                        .param("email", "ventas@bouwmaterialen.example")
                        .param("phone", "+31 6 90909090")
                        .param("address", "Havenstraat 41")
                        .param("city", "Rotterdam"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers/" + supplier.getId()));

        mockMvc.perform(post("/suppliers/{id}/archive", supplier.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers"));

        assertThat(suppliers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(companies.findAll().get(0).getId()))
                .extracting(Supplier::getName)
                .doesNotContain("Bouwmaterialen Rotterdam BV");
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.ARCHIVE);
    }

    @Test
    void suppliersPageShowsDirectorySummaryAndKeepsSearchScoped() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        suppliers.save(new Supplier(
                company,
                "Proveedor Cubiertas",
                "Karim",
                "cubiertas@example.nl",
                "+31 6 33334444",
                "Dakstraat 12",
                "Rotterdam"));
        suppliers.save(new Supplier(
                company,
                "Proveedor Madera",
                "Ilias",
                "",
                "+31 6 55556666",
                "Houtweg 8",
                "Utrecht"));
        suppliers.save(new Supplier(
                company,
                "Proveedor Pintura",
                "Sara",
                "pintura@example.nl",
                "",
                "Verfstraat 3",
                "Rotterdam"));

        mockMvc.perform(get("/suppliers").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proveedores activos")))
                .andExpect(content().string(containsString("Con email")))
                .andExpect(content().string(containsString("Con telefono")))
                .andExpect(content().string(containsString("Ciudades cubiertas")))
                .andExpect(content().string(containsString("Proveedor Cubiertas")))
                .andExpect(content().string(containsString("Proveedor Madera")))
                .andExpect(content().string(containsString("Proveedor Pintura")));

        mockMvc.perform(get("/suppliers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("q", "madera"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proveedor Madera")))
                .andExpect(content().string(not(containsString("Proveedor Cubiertas"))))
                .andExpect(content().string(not(containsString("Proveedor Pintura"))));
    }

    @Test
    void ownerActionsOnCoreOperationalModulesCreateAuditTrail() throws Exception {
        mockMvc.perform(post("/customers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Cliente Auditoria")
                        .param("email", "audit-cliente@example.nl")
                        .param("phone", "+31 6 30303030")
                        .param("address", "Straat 30")
                        .param("city", "Rotterdam"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"));

        Customer customer = customers.findAll().stream()
                .filter(item -> "Cliente Auditoria".equals(item.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/projects")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("customerId", customer.getId().toString())
                        .param("name", "Proyecto Auditoria")
                        .param("workAddress", "Obra 30")
                        .param("startDate", "2026-06-03")
                        .param("status", "IN_PROGRESS")
                        .param("budget", "2500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"));

        com.lsototalbouw.project.Project project = projects.findAll().stream()
                .filter(item -> "Proyecto Auditoria".equals(item.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/expenses")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("projectId", project.getId().toString())
                        .param("description", "Gasto auditoria")
                        .param("amount", "75.00")
                        .param("category", "Material")
                        .param("expenseDate", "2026-06-03"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        mockMvc.perform(post("/materials")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Material Auditoria")
                        .param("unit", "unidad")
                        .param("stockQuantity", "10")
                        .param("minimumStock", "2")
                        .param("unitCost", "12.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials"));

        mockMvc.perform(post("/tools")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Herramienta Auditoria")
                        .param("serialNumber", "AUD-001")
                        .param("status", "AVAILABLE")
                        .param("nextMaintenanceDate", "2026-07-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tools"));

        assertThat(auditLogs.findAll())
                .extracting(log -> log.getModuleName())
                .contains("Clientes", "Proyectos", "Gastos", "Materiales", "Herramientas");
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE);
    }

    @Test
    void operationsCanManageCalendarEventsWithFriendlyLabelsAndAuditTrail() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Calendario", "calendario@example.nl", "+31 6 24242424", "Straat 24", "Rotterdam"));
        com.lsototalbouw.project.Project project = projects.save(new com.lsototalbouw.project.Project(
                company,
                customer,
                "Proyecto calendario",
                "Planificacion de obra",
                java.time.LocalDate.now(),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("4500.00")));

        mockMvc.perform(get("/calendar").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Eventos activos")))
                .andExpect(content().string(containsString("Proximos")))
                .andExpect(content().string(containsString("Titulo")))
                .andExpect(content().string(containsString("Trabajo")))
                .andExpect(content().string(containsString("Visita")));

        mockMvc.perform(post("/calendar")
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .with(csrf())
                        .param("projectId", project.getId().toString())
                        .param("title", "Visita a obra")
                        .param("notes", "Revisar avance de techo")
                        .param("eventDate", java.time.LocalDate.now().plusDays(1).toString())
                        .param("type", "VISIT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        CalendarEvent event = calendarEvents.findByCompanyAccountIdAndActiveTrueOrderByEventDateAsc(company.getId())
                .stream()
                .filter(item -> "Visita a obra".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/calendar").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver")))
                .andExpect(content().string(containsString("Editar")))
                .andExpect(content().string(containsString("Archivar")))
                .andExpect(content().string(containsString("/calendar/" + event.getId())))
                .andExpect(content().string(containsString("/calendar/" + event.getId() + "/edit")))
                .andExpect(content().string(containsString("/calendar/" + event.getId() + "/archive")));

        mockMvc.perform(get("/calendar/{id}", event.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visita a obra")))
                .andExpect(content().string(containsString("Editar")))
                .andExpect(content().string(containsString("Archivar")));

        mockMvc.perform(get("/calendar/{id}/edit", event.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar evento")))
                .andExpect(content().string(containsString("Visita a obra")));

        mockMvc.perform(post("/calendar/{id}/edit", event.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .with(csrf())
                        .param("projectId", project.getId().toString())
                        .param("title", "Visita tecnica a obra")
                        .param("notes", "Revisar avance de techo y material")
                        .param("eventDate", java.time.LocalDate.now().plusDays(2).toString())
                        .param("type", "WORK"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/" + event.getId()));

        mockMvc.perform(get("/calendar/{id}", event.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visita tecnica a obra")))
                .andExpect(content().string(containsString("Trabajo")));

        mockMvc.perform(post("/calendar/{id}/archive", event.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        assertThat(calendarEvents.findByCompanyAccountIdAndActiveTrueOrderByEventDateAsc(company.getId()))
                .extracting(CalendarEvent::getTitle)
                .doesNotContain("Visita tecnica a obra");
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.ARCHIVE);
    }

    @Test
    void workLogsPageShowsOperationalSummaryAndInvoiceConversionAction() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Partes", "partes@example.nl", "+31 6 23232323", "Straat 23", "Rotterdam"));
        com.lsototalbouw.project.Project project = projects.save(new com.lsototalbouw.project.Project(
                company,
                customer,
                "Reforma con partes",
                "Trabajos interiores",
                java.time.LocalDate.of(2026, 6, 1),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("6000.00")));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                project,
                "INV-WORKLOG-001",
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 6, 2),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.DRAFT));
        WorkLog approvedWorkLog = workLogs.save(new WorkLog(
                company,
                project,
                java.time.LocalDate.of(2026, 6, 3),
                "Othmane",
                "Instalacion de techo",
                new java.math.BigDecimal("6.50"),
                new java.math.BigDecimal("45.00"),
                true,
                WorkLogStatus.APPROVED));
        workLogs.save(new WorkLog(
                company,
                project,
                java.time.LocalDate.of(2026, 6, 4),
                "Othmane",
                "Visita tecnica",
                new java.math.BigDecimal("1.50"),
                java.math.BigDecimal.ZERO,
                false,
                WorkLogStatus.DRAFT));

        mockMvc.perform(get("/work-logs").with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Partes visibles")))
                .andExpect(content().string(containsString("Horas totales")))
                .andExpect(content().string(containsString("Valor facturable")))
                .andExpect(content().string(containsString("EUR 292,50")))
                .andExpect(content().string(containsString("8,00")))
                .andExpect(content().string(containsString("Pendientes")));

        mockMvc.perform(get("/work-logs")
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS))
                        .param("q", "techo")
                        .param("projectId", project.getId().toString())
                        .param("status", "APPROVED")
                        .param("billable", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Instalacion de techo")))
                .andExpect(content().string(containsString("6,50")))
                .andExpect(content().string(containsString("EUR 292,50")))
                .andExpect(content().string(not(containsString("Visita tecnica"))));

        mockMvc.perform(get("/work-logs/{id}", approvedWorkLog.getId())
                        .with(user("operations@test.local").roles(SecurityRoles.OPERATIONS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Facturar horas")))
                .andExpect(content().string(containsString("Convertir en linea de factura")))
                .andExpect(content().string(containsString("INV-WORKLOG-001")))
                .andExpect(content().string(not(containsString("Convertir en l<"))));
    }

    @Test
    void expensesNormalizeCategoriesAndOfferExistingCategorySuggestions() throws Exception {
        mockMvc.perform(post("/expenses")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("description", "  Compra    madera   obra  ")
                        .param("amount", "85.50")
                        .param("category", "  Materiales    obra  ")
                        .param("expenseDate", "2026-06-02"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        mockMvc.perform(post("/expenses")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("description", "Alquiler maquinaria")
                        .param("amount", "50.00")
                        .param("category", "Maquinaria")
                        .param("expenseDate", "2026-07-10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        Expense expense = expenses.findAll().stream()
                .filter(item -> "Compra madera obra".equals(item.getDescription()))
                .findFirst()
                .orElseThrow();
        assertThat(expense.getCategory()).isEqualTo("Materiales obra");

        mockMvc.perform(get("/expenses").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("expenseCategoryOptions")))
                .andExpect(content().string(containsString("Materiales obra")));

        mockMvc.perform(get("/expenses")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("category", "Materiales obra")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compra madera obra")))
                .andExpect(content().string(containsString("Total filtrado")))
                .andExpect(content().string(containsString("EUR 85,50")))
                .andExpect(content().string(not(containsString("Alquiler maquinaria"))));
    }

    @Test
    void rejectsStateChangingRequestsWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/customers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("name", "Cliente sin CSRF"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/customers")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("name", "Cliente con CSRF"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void locksUserAfterConfiguredFailedLoginAttempts() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "owner@test.local")
                            .param("password", "bad-password"))
                    .andExpect(status().is3xxRedirection());
        }

        AppUser user = users.findByEmailIgnoreCase("owner@test.local").orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.isLoginLocked()).isTrue();
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.ACCOUNT_LOCKED);
        assertThat(notifications.findCurrentCompanyNotifications(user.getCompanyAccount().getId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getSourceKey()).isEqualTo("security:login-lock:" + user.getId());
                    assertThat(notification.getSeverity()).isEqualTo("danger");
                    assertThat(notification.getTitle()).contains("Cuenta bloqueada");
                    assertThat(notification.getMessage()).contains("owner@test.local");
                    assertThat(notification.isUnread()).isTrue();
                });
    }

    @Test
    void successfulLoginResetsFailedAttempts() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "owner@test.local")
                        .param("password", "bad-password"))
                .andExpect(status().is3xxRedirection());

        AppUser afterFailure = users.findByEmailIgnoreCase("owner@test.local").orElseThrow();
        assertThat(afterFailure.getFailedLoginAttempts()).isEqualTo(1);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "owner@test.local")
                        .param("password", "Test123!"))
                .andExpect(status().is3xxRedirection());

        AppUser afterSuccess = users.findByEmailIgnoreCase("owner@test.local").orElseThrow();
        assertThat(afterSuccess.getFailedLoginAttempts()).isZero();
        assertThat(afterSuccess.getLockedUntil()).isNull();
        assertThat(afterSuccess.getLastLoginAt()).isNotNull();
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.LOGIN_FAILURE, AuditAction.LOGIN_SUCCESS);
    }

    @Test
    void ownerCanUnlockBlockedUserFromUserManagement() throws Exception {
        AppUser blockedUser = users.findByEmailIgnoreCase("finance@test.local").orElseThrow();
        blockedUser.recordFailedLogin(1, 30);
        users.save(blockedUser);
        notifications.save(new BusinessNotification(
                blockedUser.getCompanyAccount(),
                "security:login-lock:" + blockedUser.getId(),
                "danger",
                "Cuenta bloqueada por seguridad",
                "El usuario finance@test.local ha sido bloqueado.",
                "/users",
                java.time.LocalDate.now()));

        mockMvc.perform(post("/users/{id}/unlock", blockedUser.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        AppUser unlockedUser = users.findByEmailIgnoreCase("finance@test.local").orElseThrow();
        assertThat(unlockedUser.getFailedLoginAttempts()).isZero();
        assertThat(unlockedUser.getLockedUntil()).isNull();
        assertThat(notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(
                        unlockedUser.getCompanyAccount().getId(), "security:login-lock:" + unlockedUser.getId()))
                .hasValueSatisfying(notification -> assertThat(notification.isUnread()).isFalse());
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.ACCOUNT_UNLOCKED);
    }

    @Test
    void ownerCanMarkSingleNotificationAsRead() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        BusinessNotification notification = notifications.save(new BusinessNotification(
                company,
                "test:notification:read",
                "warning",
                "Aviso de prueba",
                "Detalle de prueba",
                "/dashboard",
                java.time.LocalDate.now()));

        mockMvc.perform(post("/notifications/{id}/read", notification.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        BusinessNotification updated = notifications.findById(notification.getId()).orElseThrow();
        assertThat(updated.isUnread()).isFalse();

        mockMvc.perform(get("/notifications").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Centro de notificaciones")));
    }

    @Test
    void repeatedNotificationReadActionIsIdempotent() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        BusinessNotification notification = notifications.save(new BusinessNotification(
                company,
                "test:notification:already-read",
                "info",
                "Aviso repetido",
                "Detalle de prueba",
                "/dashboard",
                java.time.LocalDate.now()));

        mockMvc.perform(post("/notifications/{id}/read", notification.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        long auditCountAfterFirstRead = auditLogs.count();

        mockMvc.perform(post("/notifications/{id}/read", notification.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        assertThat(auditLogs.count()).isEqualTo(auditCountAfterFirstRead);
    }

    @Test
    void notificationExternalTargetUrlIsNotRenderedAsLink() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        notifications.save(new BusinessNotification(
                company,
                "test:notification:unsafe-target",
                "warning",
                "Aviso con destino inseguro",
                "Detalle de prueba",
                "https://evil.example/steal",
                java.time.LocalDate.now()));

        mockMvc.perform(get("/notifications").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aviso con destino inseguro")))
                .andExpect(content().string(containsString("href=\"/notifications\"")))
                .andExpect(content().string(not(containsString("https://evil.example/steal"))));
    }

    @Test
    void missingNotificationRedirectsWithoutSpringErrorPage() throws Exception {
        mockMvc.perform(post("/notifications/{id}/read", 99999L)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));
    }

    @Test
    void ownerCanCreateManualNotificationReminder() throws Exception {
        mockMvc.perform(post("/notifications")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Llamar cliente")
                        .param("message", "Confirmar visita de obra")
                        .param("dueDate", java.time.LocalDate.now().plusDays(1).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        assertThat(notifications.findAll())
                .extracting(BusinessNotification::getTitle)
                .contains("Llamar cliente");
        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.CREATE);
    }

    @Test
    void rejectsUnsafeDocumentUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "not-real-pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Archivo peligroso")
                        .param("category", "OTHER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solo se permiten PDF, JPG, PNG o WEBP")));

        assertThat(documents.findAll()).isEmpty();
    }

    @Test
    void rejectsOversizedDocumentUploadWithoutPersistingMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "archivo-pesado.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[10 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Archivo demasiado grande")
                        .param("category", "OTHER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El archivo no puede superar 10 MB")));

        assertThat(documents.findAll()).isEmpty();
    }

    @Test
    void ownerCanUploadDocumentFromInvoiceContext() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente documentos", "cliente-docs@test.local", "+31 6 11111111", "Calle 1", "Rotterdam"));
        com.lsototalbouw.project.Project project = new com.lsototalbouw.project.Project(
                company,
                customer,
                "Reforma con documentos",
                "Obra 12",
                java.time.LocalDate.now(),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("12000.00"));
        project = projects.save(project);
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                project,
                "DOC-2026-001",
                new java.math.BigDecimal("1500.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusDays(14),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "factura-firmada.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\nDocumento de prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Factura firmada")
                        .param("category", "INVOICE")
                        .param("invoiceId", invoice.getId().toString())
                        .param("returnUrl", "/invoices/" + invoice.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + invoice.getId()));

        var document = documents.findByCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtDesc(
                company.getId(), invoice.getId()).get(0);
        assertThat(document.getTitle()).isEqualTo("Factura firmada");
        assertThat(document.getInvoice().getId()).isEqualTo(invoice.getId());
        assertThat(document.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(document.getProject().getId()).isEqualTo(project.getId());
        assertThat(document.getSha256Checksum()).hasSize(64).matches("[a-f0-9]{64}");

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Factura firmada")));

        mockMvc.perform(get("/documents/{id}", document.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SHA-256")))
                .andExpect(content().string(containsString(document.getShortSha256Checksum())));
    }

    @Test
    void documentDownloadRejectsTamperedStoredFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento-integridad.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\nDocumento integro".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Documento con integridad")
                        .param("category", "OTHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        BusinessDocument document = documents.findAll().get(0);
        java.nio.file.Path storedFile = java.nio.file.Path.of(
                System.getProperty("java.io.tmpdir"),
                "lsototalbouw-test-documents",
                document.getStoredFilename());
        java.nio.file.Files.writeString(storedFile, "contenido alterado", java.nio.charset.StandardCharsets.UTF_8);

        mockMvc.perform(get("/documents/{id}/download", document.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("verificacion de integridad")));

        assertThat(auditLogs.findAll())
                .extracting(log -> log.getAction())
                .contains(AuditAction.DOCUMENT_INTEGRITY_FAILURE)
                .doesNotContain(AuditAction.DOWNLOAD);
        assertThat(notifications.findCurrentCompanyNotifications(document.getCompanyAccount().getId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getSourceKey()).isEqualTo("document-integrity:" + document.getId());
                    assertThat(notification.getSeverity()).isEqualTo("danger");
                    assertThat(notification.getTargetUrl()).isEqualTo("/documents/" + document.getId());
                    assertThat(notification.getMessage()).contains("SHA-256");
                    assertThat(notification.isUnread()).isTrue();
                });
    }

    @Test
    void unsafeDocumentReturnUrlIsIgnored() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente Redirect Documento", "redirect-doc@example.nl", "+31 6 13131313", "Straat 13", "Rotterdam"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento-seguro.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\nDocumento seguro".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/documents")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("customerId", customer.getId().toString())
                        .param("returnUrl", "//evil.example/steal"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("//evil.example/steal"))));

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Documento seguro")
                        .param("category", "OTHER")
                        .param("customerId", customer.getId().toString())
                        .param("returnUrl", "//evil.example/steal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));
    }

    @Test
    void documentDetailShowsUnavailableFileStateWithoutPreviewAction() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        BusinessDocument document = documents.save(new BusinessDocument(
                company,
                null,
                null,
                "Documento sin archivo",
                DocumentCategory.OTHER,
                "contrato.pdf",
                "missing-test-file.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                128,
                null));

        mockMvc.perform(get("/documents/{id}", document.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Archivo no disponible")))
                .andExpect(content().string(containsString("El registro existe, pero el archivo fisico no esta accesible")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/preview"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/download"))));
    }

    @Test
    void documentsIndexMarksUnavailableFilesForReview() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento-disponible.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\nDocumento disponible".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .with(csrf())
                        .param("title", "Documento disponible")
                        .param("category", "OTHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        BusinessDocument document = documents.save(new BusinessDocument(
                company,
                null,
                null,
                "Documento pendiente de revision",
                DocumentCategory.CONTRACT,
                "contrato-pendiente.pdf",
                "missing-index-file.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                256,
                null));

        mockMvc.perform(get("/documents").with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Requieren revision")))
                .andExpect(content().string(containsString("Documento pendiente de revision")))
                .andExpect(content().string(containsString("Revisar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/download"))));

        mockMvc.perform(get("/documents")
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER))
                        .param("unavailableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documento pendiente de revision")))
                .andExpect(content().string(containsString("documentUnavailableFilter")))
                .andExpect(content().string(containsString("checked")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Documento disponible"))));
    }

    @Test
    void relatedBusinessPagesDoNotExposeUnavailableDocumentDownloads() throws Exception {
        CompanyAccount company = companies.findAll().get(0);
        Customer customer = customers.save(new Customer(
                company, "Cliente documento relacionado", "rel-doc@example.nl", "+31 6 71717171", "Straat 71", "Rotterdam"));
        com.lsototalbouw.project.Project project = projects.save(new com.lsototalbouw.project.Project(
                company,
                customer,
                "Proyecto documento relacionado",
                "Obra con archivo no disponible",
                java.time.LocalDate.of(2026, 6, 1),
                com.lsototalbouw.common.enums.ProjectStatus.IN_PROGRESS,
                new java.math.BigDecimal("3000.00")));
        Invoice invoice = invoices.save(new Invoice(
                company,
                customer,
                project,
                "INV-DOC-MISSING-001",
                new java.math.BigDecimal("900.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 6, 1),
                java.time.LocalDate.of(2026, 6, 30),
                com.lsototalbouw.common.enums.InvoiceStatus.SENT));
        BusinessDocument document = documents.save(new BusinessDocument(
                company,
                customer,
                project,
                invoice,
                "Documento relacionado sin archivo",
                DocumentCategory.INVOICE,
                "factura-faltante.pdf",
                "missing-related-file.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                512,
                null));

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documento relacionado sin archivo")))
                .andExpect(content().string(containsString("Revisar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/download"))));

        mockMvc.perform(get("/projects/{id}", project.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documento relacionado sin archivo")))
                .andExpect(content().string(containsString("Revisar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/download"))));

        mockMvc.perform(get("/customers/{id}", customer.getId())
                        .with(user("owner@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documento relacionado sin archivo")))
                .andExpect(content().string(containsString("Revisar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/documents/" + document.getId() + "/download"))));
    }

    @Test
    void financeRoleCannotUnlockUsers() throws Exception {
        AppUser blockedUser = users.findByEmailIgnoreCase("operations@test.local").orElseThrow();
        blockedUser.recordFailedLogin(1, 30);
        users.save(blockedUser);

        mockMvc.perform(post("/users/{id}/unlock", blockedUser.getId())
                        .with(user("finance@test.local").roles(SecurityRoles.FINANCE))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        AppUser stillBlocked = users.findByEmailIgnoreCase("operations@test.local").orElseThrow();
        assertThat(stillBlocked.isLoginLocked()).isTrue();
    }

    private void createUser(CompanyAccount company, String email, String roleName) {
        Role role = roles.save(new Role("ROLE_" + roleName));
        AppUser user = new AppUser(company, email, email, passwordEncoder.encode("Test123!"));
        user.getRoles().add(role);
        users.save(user);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "audit_logs", "notifications", "work_logs", "business_documents", "calendar_events", "suppliers", "tools",
                "materials", "expenses", "payments", "invoice_payment_reminders", "invoice_lines", "invoices", "quotation_lines",
                "quotations", "projects", "customers", "user_roles", "users", "roles", "company_accounts");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
