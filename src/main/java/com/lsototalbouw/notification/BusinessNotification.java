package com.lsototalbouw.notification;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.web.SafeRedirects;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_company_source", columnNames = {"company_account_id", "source_key"}),
        indexes = {
                @Index(name = "idx_notifications_company_read", columnList = "company_account_id, read_at"),
                @Index(name = "idx_notifications_company_due", columnList = "company_account_id, due_date")
        })
public class BusinessNotification extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @Column(name = "source_key", nullable = false, length = 160)
    private String sourceKey;

    @Column(nullable = false, length = 40)
    private String severity;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_url", nullable = false, length = 240)
    private String targetUrl;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected BusinessNotification() {
    }

    public BusinessNotification(CompanyAccount companyAccount, String sourceKey, String severity, String title,
                                String message, String targetUrl, LocalDate dueDate) {
        this.companyAccount = companyAccount;
        this.sourceKey = sourceKey;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.dueDate = dueDate;
    }

    public void updateFrom(String severity, String title, String message, String targetUrl, LocalDate dueDate) {
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.dueDate = dueDate;
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatusClass() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getSafeTargetUrl() {
        return SafeRedirects.isLocalPath(targetUrl) ? targetUrl : "/notifications";
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
