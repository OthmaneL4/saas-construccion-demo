package com.lsototalbouw.audit;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_company_created", columnList = "company_account_id, created_at"),
        @Index(name = "idx_audit_company_module", columnList = "company_account_id, module_name"),
        @Index(name = "idx_audit_company_action", columnList = "company_account_id, action")
})
public class AuditLog extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(nullable = false, length = 80)
    private String moduleName;

    @Column(length = 80)
    private String entityId;

    @Column(nullable = false, length = 240)
    private String summary;

    @Column(length = 1000)
    private String details;

    protected AuditLog() {
    }

    public AuditLog(CompanyAccount companyAccount, AppUser user, AuditAction action, String moduleName,
                    String entityId, String summary, String details) {
        this.companyAccount = companyAccount;
        this.user = user;
        this.action = action;
        this.moduleName = moduleName;
        this.entityId = entityId;
        this.summary = summary;
        this.details = details;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public AppUser getUser() {
        return user;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }
}
