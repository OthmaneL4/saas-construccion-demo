package com.lsototalbouw.audit;

import com.lsototalbouw.company.CompanyContextService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditController {

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogs;
    private final CompanyContextService companyContext;

    public AuditController(AuditLogRepository auditLogs, CompanyContextService companyContext) {
        this.auditLogs = auditLogs;
        this.companyContext = companyContext;
    }

    @GetMapping("/audit")
    public String index(Model model,
                        @RequestParam(name = "action", required = false) AuditAction action,
                        @RequestParam(name = "module", required = false) String module,
                        @RequestParam(name = "user", required = false) String user,
                        @RequestParam(name = "from", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(name = "to", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long companyId = companyContext.currentCompanyId();
        String cleanModule = clean(module);
        String cleanUser = clean(user);
        List<AuditLog> logs = search(companyId, action, cleanModule, cleanUser, from, to, 200);
        model.addAttribute("pageTitle", "Auditor");
        model.addAttribute("auditLogs", logs);
        model.addAttribute("summary", AuditTrailSummary.from(logs));
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("modules", auditLogs.findDistinctModuleNamesByCompanyId(companyId));
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedModule", cleanModule);
        model.addAttribute("selectedUser", cleanUser);
        model.addAttribute("selectedFrom", from);
        model.addAttribute("selectedTo", to);
        return "audit/index";
    }

    @GetMapping("/audit/export")
    public ResponseEntity<byte[]> export(@RequestParam(name = "action", required = false) AuditAction action,
                                         @RequestParam(name = "module", required = false) String module,
                                         @RequestParam(name = "user", required = false) String user,
                                         @RequestParam(name = "from", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(name = "to", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long companyId = companyContext.currentCompanyId();
        List<AuditLog> logs = search(companyId, action, clean(module), clean(user), from, to, 1000);
        byte[] content = toCsv(logs).getBytes(StandardCharsets.UTF_8);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("audit-log.csv")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(content);
    }

    private List<AuditLog> search(Long companyId, AuditAction action, String module, String user,
                                  LocalDate from, LocalDate to, int limit) {
        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        return auditLogs.searchByCompanyId(companyId, action, module, user, fromDate, toDate, PageRequest.of(0, limit));
    }

    private String toCsv(List<AuditLog> logs) {
        StringBuilder csv = new StringBuilder("Fecha,Usuario,Accion,Modulo,Registro,Resumen,Detalle\n");
        for (AuditLog log : logs) {
            csv.append(csvField(log.getCreatedAt() == null ? "" : CSV_DATE_FORMAT.format(log.getCreatedAt())))
                    .append(',')
                    .append(csvField(log.getUser() == null ? "-" : log.getUser().getEmail()))
                    .append(',')
                    .append(csvField(log.getAction()))
                    .append(',')
                    .append(csvField(log.getModuleName()))
                    .append(',')
                    .append(csvField(log.getEntityId()))
                    .append(',')
                    .append(csvField(log.getSummary()))
                    .append(',')
                    .append(csvField(log.getDetails()))
                    .append('\n');
        }
        return csv.toString();
    }

    private String csvField(Object value) {
        String text = value == null ? "" : value.toString();
        text = text.replace('\r', ' ').replace('\n', ' ');
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
