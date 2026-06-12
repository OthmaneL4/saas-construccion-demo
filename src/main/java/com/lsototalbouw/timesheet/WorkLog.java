package com.lsototalbouw.timesheet;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.invoice.InvoiceLine;
import com.lsototalbouw.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "work_logs", indexes = {
        @Index(name = "idx_work_logs_company_active_date", columnList = "company_account_id, active, work_date"),
        @Index(name = "idx_work_logs_project", columnList = "project_id"),
        @Index(name = "idx_work_logs_company_status", columnList = "company_account_id, status")
})
public class WorkLog extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, length = 160)
    private String workerName;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal hours = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean billable = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_line_id")
    private InvoiceLine invoiceLine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkLogStatus status = WorkLogStatus.DRAFT;

    protected WorkLog() {
    }

    public WorkLog(CompanyAccount companyAccount, Project project, LocalDate workDate, String workerName,
                   String description, BigDecimal hours, BigDecimal hourlyRate, boolean billable,
                   WorkLogStatus status) {
        this.companyAccount = companyAccount;
        this.project = project;
        this.workDate = workDate;
        this.workerName = workerName;
        this.description = description;
        this.hours = normalize(hours);
        this.hourlyRate = normalize(hourlyRate);
        this.billable = billable;
        this.status = status;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public Project getProject() {
        return project;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public boolean isBillable() {
        return billable;
    }

    public WorkLogStatus getStatus() {
        return status;
    }

    public InvoiceLine getInvoiceLine() {
        return invoiceLine;
    }

    public boolean isInvoiced() {
        return invoiceLine != null || status == WorkLogStatus.INVOICED;
    }

    public BigDecimal getLineTotal() {
        return hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    public void updateFrom(Project project, LocalDate workDate, String workerName, String description,
                           BigDecimal hours, BigDecimal hourlyRate, boolean billable, WorkLogStatus status) {
        this.project = project;
        this.workDate = workDate;
        this.workerName = workerName;
        this.description = description;
        this.hours = normalize(hours);
        this.hourlyRate = normalize(hourlyRate);
        this.billable = billable;
        this.status = status;
    }

    public void markInvoiced(InvoiceLine invoiceLine) {
        this.invoiceLine = invoiceLine;
        this.status = WorkLogStatus.INVOICED;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
