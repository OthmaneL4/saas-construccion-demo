package com.lsototalbouw.config;

import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.common.enums.PaymentStatus;
import com.lsototalbouw.common.enums.ProjectStatus;
import com.lsototalbouw.common.enums.QuotationStatus;
import com.lsototalbouw.common.enums.ToolStatus;
import com.lsototalbouw.calendar.CalendarEvent;
import com.lsototalbouw.calendar.CalendarEventRepository;
import com.lsototalbouw.calendar.CalendarEventType;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import com.lsototalbouw.document.BusinessDocument;
import com.lsototalbouw.document.BusinessDocumentRepository;
import com.lsototalbouw.document.DocumentCategory;
import com.lsototalbouw.expense.Expense;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceLine;
import com.lsototalbouw.invoice.InvoiceLineRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.material.MaterialItem;
import com.lsototalbouw.material.MaterialRepository;
import com.lsototalbouw.notification.BusinessNotification;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.payment.Payment;
import com.lsototalbouw.payment.PaymentRepository;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.quotation.Quotation;
import com.lsototalbouw.quotation.QuotationLine;
import com.lsototalbouw.quotation.QuotationLineRepository;
import com.lsototalbouw.quotation.QuotationRepository;
import com.lsototalbouw.security.SecurityRoles;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"dev", "demo"})
public class DataInitializer implements CommandLineRunner {

    private final CompanyAccountRepository companies;
    private final RoleRepository roles;
    private final AppUserRepository users;
    private final CustomerRepository customers;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final ExpenseRepository expenses;
    private final MaterialRepository materials;
    private final ToolRepository tools;
    private final SupplierRepository suppliers;
    private final PaymentRepository payments;
    private final CalendarEventRepository calendarEvents;
    private final WorkLogRepository workLogs;
    private final QuotationRepository quotations;
    private final QuotationLineRepository quotationLines;
    private final BusinessDocumentRepository documents;
    private final BusinessNotificationRepository notifications;
    private final PasswordEncoder passwordEncoder;
    private final Path documentsDirectory;
    private final String demoUserFullName;
    private final String demoUserEmail;
    private final String demoUserPassword;

    public DataInitializer(CompanyAccountRepository companies, RoleRepository roles, AppUserRepository users,
                            CustomerRepository customers, ProjectRepository projects, InvoiceRepository invoices,
                            InvoiceLineRepository invoiceLines, ExpenseRepository expenses, MaterialRepository materials,
                            ToolRepository tools, SupplierRepository suppliers, PaymentRepository payments,
                            CalendarEventRepository calendarEvents, WorkLogRepository workLogs,
                            QuotationRepository quotations, QuotationLineRepository quotationLines,
                            BusinessDocumentRepository documents, BusinessNotificationRepository notifications,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.upload.documents-dir:uploads/documents}") String documentsDir,
                            @Value("${app.demo-login.full-name:LSOTOTALBOUW Demo User}") String demoUserFullName,
                            @Value("${app.demo-login.username:demo@lsototalbouw.nl}") String demoUserEmail,
                            @Value("${app.demo-login.password:Demo2026!}") String demoUserPassword) {
        this.companies = companies;
        this.roles = roles;
        this.users = users;
        this.customers = customers;
        this.projects = projects;
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.expenses = expenses;
        this.materials = materials;
        this.tools = tools;
        this.suppliers = suppliers;
        this.payments = payments;
        this.calendarEvents = calendarEvents;
        this.workLogs = workLogs;
        this.quotations = quotations;
        this.quotationLines = quotationLines;
        this.documents = documents;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.documentsDirectory = Paths.get(documentsDir).toAbsolutePath().normalize();
        this.demoUserFullName = demoUserFullName;
        this.demoUserEmail = demoUserEmail;
        this.demoUserPassword = demoUserPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role owner = ensureRole(SecurityRoles.OWNER);
        Role admin = ensureRole(SecurityRoles.ADMIN);
        ensureRole(SecurityRoles.FINANCE);
        ensureRole(SecurityRoles.OPERATIONS);
        ensureRole(SecurityRoles.INVENTORY);
        ensureRole(SecurityRoles.DOCUMENTS);
        ensureRole(SecurityRoles.AUDITOR);

        CompanyAccount company = companies.findAll().stream()
                .findFirst()
                .orElseGet(() -> companies.save(new CompanyAccount(
                        "LSOTOTALBOUW", "NL001234567B01", "info@lsototalbouw.nl",
                        "+31 6 12345678", "Rotterdam, Nederland")));

        users.findByEmailIgnoreCase(demoUserEmail).orElseGet(() -> {
            AppUser user = new AppUser(company, demoUserFullName, demoUserEmail,
                    passwordEncoder.encode(demoUserPassword));
            user.getRoles().add(owner);
            user.getRoles().add(admin);
            return users.save(user);
        });

        seedDemoBusiness(company);
    }

    private void seedDemoBusiness(CompanyAccount company) {
        Customer familieJansen = customer(company, "Familie Jansen", "jansen@example.nl",
                "+31 6 11111111", "Kerkstraat 12", "Utrecht");
        Customer bouwgroep = customer(company, "Bouwgroep Van Dijk", "planning@vandijkbouw.nl",
                "+31 10 2222222", "Havenweg 8", "Rotterdam");
        Customer horeca = customer(company, "Horeca Plaza Noord", "beheer@plazanoord.nl",
                "+31 20 3333333", "Marktplein 4", "Amsterdam");
        Customer architecten = customer(company, "Architectenbureau De Maas", "office@demaas-architecten.nl",
                "+31 10 7788990", "Wijnhaven 21", "Rotterdam");
        Customer vveParkzicht = customer(company, "VvE Parkzicht", "bestuur@vveparkzicht.nl",
                "+31 70 4422110", "Parklaan 34", "Den Haag");

        Project roof = project(company, familieJansen, "Dakrenovatie woonhuis",
                "Kerkstraat 12, Utrecht", LocalDate.now().minusDays(12), ProjectStatus.IN_PROGRESS,
                new BigDecimal("8500.00"));
        Project carpentry = project(company, bouwgroep, "Interieur timmerwerk kantoor",
                "Havenweg 8, Rotterdam", LocalDate.now().plusDays(5), ProjectStatus.PLANNED,
                new BigDecimal("12400.00"));
        Project repair = project(company, horeca, "Spoedreparatie lekkage keuken",
                "Marktplein 4, Amsterdam", LocalDate.now().minusDays(3), ProjectStatus.IN_PROGRESS,
                new BigDecimal("2950.00"));
        Project bathroom = project(company, architecten, "Badkamer renovatie appartement",
                "Wijnhaven 21, Rotterdam", LocalDate.now().minusDays(34), ProjectStatus.COMPLETED,
                new BigDecimal("6800.00"));
        Project facade = project(company, vveParkzicht, "Gevelherstel entreecomplex",
                "Parklaan 34, Den Haag", LocalDate.now().plusDays(12), ProjectStatus.PLANNED,
                new BigDecimal("18900.00"));

        Invoice firstInvoice = invoice(company, familieJansen, roof, "2026-0001", new BigDecimal("3500.00"),
                new BigDecimal("1500.00"), LocalDate.now().minusDays(10), LocalDate.now().plusDays(4),
                InvoiceStatus.PARTIALLY_PAID);
        Invoice secondInvoice = invoice(company, bouwgroep, carpentry, "2026-0002", new BigDecimal("2200.00"),
                BigDecimal.ZERO, LocalDate.now().minusDays(3), LocalDate.now().plusDays(21), InvoiceStatus.SENT);
        Invoice overdueInvoice = invoice(company, horeca, repair, "2026-0003", new BigDecimal("1450.00"),
                BigDecimal.ZERO, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5), InvoiceStatus.OVERDUE);
        Invoice paidInvoice = invoice(company, architecten, bathroom, "2026-0004", new BigDecimal("6800.00"),
                new BigDecimal("6800.00"), LocalDate.now().minusDays(25), LocalDate.now().minusDays(11),
                InvoiceStatus.PAID);

        invoiceLine(firstInvoice, "Voorschot dakrenovatie en steigerwerk",
                new BigDecimal("1.00"), new BigDecimal("2892.56"), new BigDecimal("21.00"));
        invoiceLine(secondInvoice, "Voorbereiding timmerwerk kantoor",
                new BigDecimal("1.00"), new BigDecimal("1818.18"), new BigDecimal("21.00"));
        invoiceLine(overdueInvoice, "Spoedreparatie lekkage en materiaal",
                new BigDecimal("1.00"), new BigDecimal("1198.35"), new BigDecimal("21.00"));
        invoiceLine(paidInvoice, "Complete renovatie badkamer inclusief afwerking",
                new BigDecimal("1.00"), new BigDecimal("5619.83"), new BigDecimal("21.00"));

        expense(company, roof, "Dakpannen en isolatiemateriaal", new BigDecimal("940.50"),
                "Materialen", LocalDate.now().minusDays(8));
        expense(company, carpentry, "Huur steiger", new BigDecimal("320.00"),
                "Materieel", LocalDate.now().minusDays(2));
        expense(company, repair, "Noodmateriaal lekkage", new BigDecimal("185.75"),
                "Reparaties", LocalDate.now().minusDays(3));
        expense(company, bathroom, "Sanitair en tegelwerk badkamer", new BigDecimal("2140.00"),
                "Materialen", LocalDate.now().minusDays(28));
        expense(company, facade, "Inspectierapport gevel en hoogwerker reservering", new BigDecimal("475.00"),
                "Voorbereiding", LocalDate.now().minusDays(1));

        material(company, "Dakpannen", "stuks", 38, 50, new BigDecimal("2.80"));
        material(company, "Hout balken 45x70", "meter", 180, 40, new BigDecimal("4.20"));
        material(company, "Isolatieplaten", "m2", 24, 30, new BigDecimal("12.50"));
        material(company, "Schroeven RVS", "doos", 14, 10, new BigDecimal("18.90"));
        material(company, "Gipsplaten vochtwerend", "stuks", 32, 20, new BigDecimal("11.75"));
        material(company, "EPDM dakfolie", "m2", 18, 25, new BigDecimal("16.40"));

        tool(company, "Accuboormachine Makita", "MK-2026-001", ToolStatus.IN_USE,
                LocalDate.now().plusMonths(2));
        tool(company, "Cirkelzaag DeWalt", "DW-2026-014", ToolStatus.AVAILABLE,
                LocalDate.now().plusMonths(1));
        tool(company, "Laser meetinstrument", "LS-2026-003", ToolStatus.MAINTENANCE,
                LocalDate.now().plusDays(7));
        tool(company, "Tegelzaag Rubi", "RB-2026-022", ToolStatus.AVAILABLE,
                LocalDate.now().plusMonths(3));
        tool(company, "Dakbrander Sievert", "SV-2026-009", ToolStatus.IN_USE,
                LocalDate.now().plusDays(21));

        supplier(company, "Bouwmaat Rotterdam", "Sander de Vries",
                "rotterdam@bouwmaat.nl", "+31 10 4444444", "Industrieweg 18", "Rotterdam");
        supplier(company, "Dakservice Nederland", "Eva Bakker",
                "planning@dakservice.nl", "+31 30 5555555", "Ambachtstraat 7", "Utrecht");
        supplier(company, "Tegelcentrum Zuid-Holland", "Milan Visser",
                "orders@tegelcentrumzh.nl", "+31 70 6611223", "Keramiekstraat 5", "Den Haag");

        payment(company, firstInvoice, new BigDecimal("1500.00"),
                LocalDate.now().minusDays(5), "Bank transfer", PaymentStatus.COMPLETED);
        payment(company, secondInvoice, new BigDecimal("500.00"),
                LocalDate.now().plusDays(7), "Expected bank transfer", PaymentStatus.PENDING);
        payment(company, paidInvoice, new BigDecimal("6800.00"),
                LocalDate.now().minusDays(12), "Bank transfer", PaymentStatus.COMPLETED);

        calendarEvent(company, roof, "Controle dakconstructie",
                "Controle op isolatie, waterdichtheid en afwerking.", LocalDate.now().plusDays(1), CalendarEventType.VISIT);
        calendarEvent(company, carpentry, "Start timmerwerk kantoor",
                "Materiaal meenemen en werkplaatsindeling bespreken.", LocalDate.now().plusDays(5), CalendarEventType.WORK);
        calendarEvent(company, null, "Onderhoud laser meetinstrument",
                "Kalibratie controleren voor volgende meetklus.", LocalDate.now().plusDays(7), CalendarEventType.MAINTENANCE);
        calendarEvent(company, facade, "Voorinspectie gevelherstel",
                "Opname scheurvorming, bereikbaarheid en veiligheidsplan.", LocalDate.now().plusDays(10), CalendarEventType.VISIT);

        workLog(company, roof, LocalDate.now().minusDays(2), "Othmane",
                "Dakpannen verwijderd en onderconstructie gecontroleerd", new BigDecimal("7.50"),
                new BigDecimal("52.50"), true, WorkLogStatus.APPROVED);
        workLog(company, repair, LocalDate.now().minusDays(1), "Othmane",
                "Noodreparatie lekkage en afdichting keuken", new BigDecimal("4.00"),
                new BigDecimal("65.00"), true, WorkLogStatus.APPROVED);
        workLog(company, bathroom, LocalDate.now().minusDays(18), "Othmane",
                "Tegelwerk geplaatst en kitnaden afgewerkt", new BigDecimal("8.00"),
                new BigDecimal("55.00"), true, WorkLogStatus.INVOICED);

        Quotation storageQuote = quotation(company, horeca, "OFF-2026-0001",
                "Renovatie opslagruimte", "Wanden herstellen, vloer voorbereiden en nieuw plaatmateriaal plaatsen.",
                new BigDecimal("4840.00"), LocalDate.now().minusDays(2), LocalDate.now().plusDays(14),
                QuotationStatus.SENT);
        quotationLine(storageQuote, "Arbeid renovatie opslagruimte",
                new BigDecimal("48.00"), new BigDecimal("55.00"), new BigDecimal("21.00"));
        quotationLine(storageQuote, "Plaatmateriaal en bevestiging",
                new BigDecimal("1.00"), new BigDecimal("1360.00"), new BigDecimal("21.00"));

        Quotation facadeQuote = quotation(company, vveParkzicht, "OFF-2026-0002",
                "Gevelherstel entreecomplex", "Voegwerk herstellen, scheuren repareren en entree opnieuw afwerken.",
                new BigDecimal("18900.00"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(21),
                QuotationStatus.SENT);
        quotationLine(facadeQuote, "Arbeid gevelherstel en voegwerk",
                new BigDecimal("120.00"), new BigDecimal("62.50"), new BigDecimal("21.00"));
        quotationLine(facadeQuote, "Materiaal, hoogwerker en veiligheidsvoorzieningen",
                new BigDecimal("1.00"), new BigDecimal("8125.00"), new BigDecimal("21.00"));

        document(company, familieJansen, roof, firstInvoice, "Contrato firmado dakrenovatie",
                DocumentCategory.CONTRACT, "contrato-dakrenovatie-jansen.pdf",
                "demo-contrato-dakrenovatie-jansen.pdf",
                "Contrato firmado para el trabajo de renovacion de cubierta.");
        document(company, horeca, repair, overdueInvoice, "Informe de reparacion urgente",
                DocumentCategory.CUSTOMER_DOCUMENT, "informe-reparacion-horeca-plaza.pdf",
                "demo-informe-reparacion-horeca-plaza.pdf",
                "Informe tecnico de la intervencion por filtracion en cocina.");
        document(company, vveParkzicht, facade, null, "Fotos iniciales gevelherstel",
                DocumentCategory.PROJECT_PHOTO, "fotos-gevelherstel-parkzicht.pdf",
                "demo-fotos-gevelherstel-parkzicht.pdf",
                "Paquete demo con fotos iniciales y notas de inspeccion.");

        notification(company, "demo:overdue-invoice:2026-0003", "danger",
                "Factura vencida pendiente",
                "La factura 2026-0003 de Horeca Plaza Noord esta vencida y requiere seguimiento.",
                "/invoices/" + overdueInvoice.getId(), overdueInvoice.getDueDate());
        notification(company, "demo:low-stock:dakpannen", "warning",
                "Stock bajo de dakpannen",
                "Quedan pocas unidades disponibles para trabajos de cubierta planificados.",
                "/materials", LocalDate.now().plusDays(2));
        notification(company, "demo:site-visit:facade", "info",
                "Visita de obra programada",
                "Revisar accesos y seguridad antes de la inspeccion de gevelherstel.",
                "/calendar", LocalDate.now().plusDays(10));
    }

    private Role ensureRole(String roleName) {
        String persistedName = "ROLE_" + roleName;
        return roles.findByName(persistedName).orElseGet(() -> roles.save(new Role(persistedName)));
    }

    private Customer customer(CompanyAccount company, String name, String email, String phone, String address, String city) {
        return customers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(company.getId()).stream()
                .filter(customer -> customer.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> customers.save(new Customer(company, name, email, phone, address, city)));
    }

    private Project project(CompanyAccount company, Customer customer, String name, String workAddress,
                            LocalDate startDate, ProjectStatus status, BigDecimal budget) {
        return projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(company.getId()).stream()
                .filter(project -> project.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> projects.save(new Project(company, customer, name, workAddress, startDate, status, budget)));
    }

    private Invoice invoice(CompanyAccount company, Customer customer, Project project, String invoiceNumber,
                            BigDecimal amount, BigDecimal paidAmount, LocalDate issueDate, LocalDate dueDate,
                            InvoiceStatus status) {
        return invoices.findByCompanyAccountIdAndInvoiceNumberIgnoreCase(company.getId(), invoiceNumber)
                .orElseGet(() -> invoices.save(new Invoice(company, customer, project, invoiceNumber, amount, paidAmount,
                        issueDate, dueDate, status)));
    }

    private InvoiceLine invoiceLine(Invoice invoice, String description, BigDecimal quantity, BigDecimal unitPrice,
                                    BigDecimal vatRate) {
        return invoiceLines.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderByCreatedAtAsc(
                        invoice.getCompanyAccount().getId(), invoice.getId()).stream()
                .filter(line -> line.getDescription().equalsIgnoreCase(description))
                .findFirst()
                .orElseGet(() -> invoiceLines.save(new InvoiceLine(invoice, description, quantity, unitPrice, vatRate)));
    }

    private Expense expense(CompanyAccount company, Project project, String description, BigDecimal amount,
                            String category, LocalDate expenseDate) {
        return expenses.findByCompanyAccountIdAndActiveTrueOrderByExpenseDateDesc(company.getId()).stream()
                .filter(expense -> expense.getDescription().equalsIgnoreCase(description))
                .findFirst()
                .orElseGet(() -> expenses.save(new Expense(company, project, description, amount, category, expenseDate)));
    }

    private MaterialItem material(CompanyAccount company, String name, String unit, int stockQuantity,
                                  int minimumStock, BigDecimal unitCost) {
        return materials.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(company.getId()).stream()
                .filter(material -> material.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> materials.save(new MaterialItem(company, name, unit, stockQuantity, minimumStock, unitCost)));
    }

    private ToolItem tool(CompanyAccount company, String name, String serialNumber,
                          ToolStatus status, LocalDate nextMaintenanceDate) {
        return tools.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(company.getId()).stream()
                .filter(tool -> serialNumber.equalsIgnoreCase(tool.getSerialNumber()))
                .findFirst()
                .orElseGet(() -> tools.save(new ToolItem(company, name, serialNumber, status, nextMaintenanceDate)));
    }

    private Supplier supplier(CompanyAccount company, String name, String contactName, String email,
                              String phone, String address, String city) {
        return suppliers.findByCompanyAccountIdAndActiveTrueOrderByNameAsc(company.getId()).stream()
                .filter(supplier -> supplier.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> suppliers.save(new Supplier(company, name, contactName, email, phone, address, city)));
    }

    private Payment payment(CompanyAccount company, Invoice invoice, BigDecimal amount, LocalDate paymentDate,
                            String method, PaymentStatus status) {
        return payments.findByCompanyAccountIdAndActiveTrueOrderByPaymentDateDesc(company.getId()).stream()
                .filter(payment -> payment.getInvoice().getId().equals(invoice.getId())
                        && payment.getAmount().compareTo(amount) == 0
                        && payment.getPaymentDate().equals(paymentDate))
                .findFirst()
                .orElseGet(() -> payments.save(new Payment(company, invoice, amount, paymentDate, method, status)));
    }

    private CalendarEvent calendarEvent(CompanyAccount company, Project project, String title, String description,
                                        LocalDate eventDate, CalendarEventType type) {
        return calendarEvents.findByCompanyAccountIdAndActiveTrueOrderByEventDateAsc(company.getId()).stream()
                .filter(event -> event.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElseGet(() -> calendarEvents.save(new CalendarEvent(company, project, title, description, eventDate, type)));
    }

    private WorkLog workLog(CompanyAccount company, Project project, LocalDate workDate, String workerName,
                            String description, BigDecimal hours, BigDecimal hourlyRate, boolean billable,
                            WorkLogStatus status) {
        return workLogs.findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByWorkDateDescCreatedAtDesc(
                        company.getId(), project.getId()).stream()
                .filter(workLog -> workLog.getDescription().equalsIgnoreCase(description)
                        && workLog.getWorkDate().equals(workDate))
                .findFirst()
                .orElseGet(() -> workLogs.save(new WorkLog(company, project, workDate, workerName, description,
                        hours, hourlyRate, billable, status)));
    }

    private Quotation quotation(CompanyAccount company, Customer customer, String quotationNumber, String title,
                                String description, BigDecimal amount, LocalDate issueDate, LocalDate expiryDate,
                                QuotationStatus status) {
        return quotations.findByCompanyAccountIdAndQuotationNumberIgnoreCase(company.getId(), quotationNumber)
                .orElseGet(() -> quotations.save(new Quotation(company, customer, quotationNumber, title, description,
                        amount, issueDate, expiryDate, status)));
    }

    private QuotationLine quotationLine(Quotation quotation, String description, BigDecimal quantity,
                                        BigDecimal unitPrice, BigDecimal vatRate) {
        return quotationLines.findByQuotationCompanyAccountIdAndQuotationIdAndActiveTrueOrderByCreatedAtAsc(
                        quotation.getCompanyAccount().getId(), quotation.getId()).stream()
                .filter(line -> line.getDescription().equalsIgnoreCase(description))
                .findFirst()
                .orElseGet(() -> quotationLines.save(new QuotationLine(quotation, description, quantity, unitPrice, vatRate)));
    }

    private BusinessDocument document(CompanyAccount company, Customer customer, Project project, Invoice invoice,
                                      String title, DocumentCategory category, String originalFilename,
                                      String storedFilename, String notes) {
        byte[] fileContent = ensureDemoPdf(storedFilename, title, notes);
        return documents.findByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(company.getId()).stream()
                .filter(document -> storedFilename.equalsIgnoreCase(document.getStoredFilename()))
                .findFirst()
                .orElseGet(() -> documents.save(new BusinessDocument(company, customer, project, invoice, title,
                        category, originalFilename, storedFilename, "application/pdf", fileContent.length, notes)));
    }

    private BusinessNotification notification(CompanyAccount company, String sourceKey, String severity, String title,
                                              String message, String targetUrl, LocalDate dueDate) {
        return notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(company.getId(), sourceKey)
                .map(existing -> {
                    existing.updateFrom(severity, title, message, targetUrl, dueDate);
                    return existing;
                })
                .orElseGet(() -> notifications.save(new BusinessNotification(
                        company, sourceKey, severity, title, message, targetUrl, dueDate)));
    }

    private byte[] ensureDemoPdf(String storedFilename, String title, String notes) {
        try {
            Files.createDirectories(documentsDirectory);
            Path target = documentsDirectory.resolve(storedFilename).normalize();
            if (!target.startsWith(documentsDirectory)) {
                throw new IllegalArgumentException("Ruta de documento demo no permitida");
            }
            byte[] content = minimalPdf(title, notes);
            if (!Files.isRegularFile(target)) {
                Files.write(target, content);
            }
            return content;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo crear el documento demo", ex);
        }
    }

    private byte[] minimalPdf(String title, String notes) {
        String safeTitle = pdfText(title);
        String safeNotes = pdfText(notes);
        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "5 0 obj << /Length 170 >> stream\n"
                + "BT /F1 18 Tf 72 720 Td (" + safeTitle + ") Tj "
                + "/F1 11 Tf 0 -32 Td (LSOTOTALBOUW demo document) Tj "
                + "0 -22 Td (" + safeNotes + ") Tj ET\n"
                + "endstream endobj\n"
                + "xref\n0 6\n0000000000 65535 f \n"
                + "trailer << /Root 1 0 R /Size 6 >>\nstartxref\n0\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }

    private String pdfText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "");
    }
}
