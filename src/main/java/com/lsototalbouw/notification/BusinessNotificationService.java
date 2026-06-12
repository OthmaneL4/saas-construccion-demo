package com.lsototalbouw.notification;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.dashboard.OperationalAlert;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessNotificationService {

    private final BusinessNotificationRepository notifications;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public BusinessNotificationService(BusinessNotificationRepository notifications,
                                       CompanyContextService companyContext,
                                       AuditService auditService) {
        this.notifications = notifications;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BusinessNotification> listCurrentCompany() {
        return notifications.findCurrentCompanyNotifications(companyContext.currentCompanyId());
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notifications.countByCompanyAccountIdAndReadAtIsNullAndActiveTrue(companyContext.currentCompanyId());
    }

    @Transactional
    public void syncFromAlerts(List<OperationalAlert> alerts) {
        CompanyAccount company = companyContext.currentCompany();
        Long companyId = company.getId();
        for (OperationalAlert alert : alerts) {
            notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(companyId, alert.sourceKey())
                    .ifPresentOrElse(existing -> existing.updateFrom(
                                    alert.severity(), alert.title(), alert.message(), alert.href(), alert.dueDate()),
                            () -> notifications.save(new BusinessNotification(
                                    company, alert.sourceKey(), alert.severity(), alert.title(),
                                    alert.message(), alert.href(), alert.dueDate())));
        }
    }

    @Transactional
    public BusinessNotification createManualReminder(BusinessNotificationForm form) {
        CompanyAccount company = companyContext.currentCompany();
        BusinessNotification notification = notifications.save(new BusinessNotification(
                company,
                "manual:" + UUID.randomUUID(),
                "info",
                trim(form.getTitle(), 160),
                trim(form.getMessage(), 500),
                "/notifications",
                form.getDueDate()
        ));
        auditService.record(AuditAction.CREATE, "NOTIFICATIONS", notification.getId(),
                "Recordatorio creado", notification.getTitle());
        return notification;
    }

    @Transactional
    public boolean markAsRead(Long id) {
        BusinessNotification notification = notifications
                .findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        if (!notification.isUnread()) {
            return false;
        }
        notification.markAsRead();
        auditService.record(AuditAction.UPDATE, "NOTIFICATIONS", id,
                "Notificacion marcada como leida", notification.getTitle());
        return true;
    }

    @Transactional
    public void markAllAsRead() {
        Long companyId = companyContext.currentCompanyId();
        List<BusinessNotification> unread = notifications.findByCompanyAccountIdAndReadAtIsNullAndActiveTrue(companyId);
        unread.forEach(BusinessNotification::markAsRead);
        if (!unread.isEmpty()) {
            auditService.record(AuditAction.UPDATE, "NOTIFICATIONS", null,
                    "Notificaciones marcadas como leidas", unread.size() + " notificaciones");
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
