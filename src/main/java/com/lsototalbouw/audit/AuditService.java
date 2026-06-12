package com.lsototalbouw.audit;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists security and business audit events for the current tenant.
 *
 * <p>Audit writes run in a new transaction so important events can be stored even when the surrounding
 * business transaction later rolls back. The service resolves the authenticated user when available and
 * trims free-text fields to the database limits before persisting the audit record.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final CompanyContextService companyContext;
    private final AppUserRepository users;

    public AuditService(AuditLogRepository auditLogs, CompanyContextService companyContext,
                        AppUserRepository users) {
        this.auditLogs = auditLogs;
        this.companyContext = companyContext;
        this.users = users;
    }

    /**
     * Records an audit event without extended details.
     *
     * @param action     categorized action that occurred
     * @param moduleName functional module where the event originated
     * @param entityId   optional affected entity identifier
     * @param summary    concise event summary
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String moduleName, Long entityId, String summary) {
        record(action, moduleName, entityId, summary, null);
    }

    /**
     * Records an audit event with optional extended details.
     *
     * @param action     categorized action that occurred
     * @param moduleName functional module where the event originated
     * @param entityId   optional affected entity identifier
     * @param summary    concise event summary
     * @param details    optional contextual details for investigation or traceability
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String moduleName, Long entityId, String summary, String details) {
        CompanyAccount company = companyContext.currentCompany();
        AppUser user = currentUser();
        AuditLog log = new AuditLog(
                company,
                user,
                action,
                moduleName,
                entityId == null ? null : entityId.toString(),
                trim(summary, 240),
                trim(details, 1000)
        );
        auditLogs.save(log);
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
