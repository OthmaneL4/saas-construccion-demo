package com.lsototalbouw.invoice;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.enums.InvoiceStatus;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.quotation.Quotation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoices_company_number", columnNames = {"company_account_id", "invoice_number"}),
        indexes = {
                @Index(name = "idx_invoices_company_active_due", columnList = "company_account_id, active, due_date"),
                @Index(name = "idx_invoices_company_status_active", columnList = "company_account_id, status, active"),
                @Index(name = "idx_invoices_customer", columnList = "customer_id"),
                @Index(name = "idx_invoices_project", columnList = "project_id"),
                @Index(name = "idx_invoices_quotation", columnList = "quotation_id"),
                @Index(name = "idx_invoices_company_reminder", columnList = "company_account_id, last_payment_reminder_at")
        })
public class Invoice extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    private LocalDate issueDate;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "last_payment_reminder_at")
    private LocalDateTime lastPaymentReminderAt;

    @Column(name = "payment_reminder_count", nullable = false)
    private int paymentReminderCount;

    protected Invoice() {
    }

    public Invoice(CompanyAccount companyAccount, Customer customer, Project project, String invoiceNumber,
                   BigDecimal amount, BigDecimal paidAmount, LocalDate issueDate, LocalDate dueDate,
                   InvoiceStatus status) {
        this(companyAccount, customer, project, invoiceNumber, amount, paidAmount, issueDate, dueDate, status, null);
    }

    public Invoice(CompanyAccount companyAccount, Customer customer, Project project, String invoiceNumber,
                   BigDecimal amount, BigDecimal paidAmount, LocalDate issueDate, LocalDate dueDate,
                   InvoiceStatus status, Quotation quotation) {
        this.companyAccount = companyAccount;
        this.customer = customer;
        this.project = project;
        this.quotation = quotation;
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Project getProject() {
        return project;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getOutstandingAmount() {
        BigDecimal outstanding = amount.subtract(paidAmount);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastPaymentReminderAt() {
        return lastPaymentReminderAt;
    }

    public int getPaymentReminderCount() {
        return paymentReminderCount;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public void updateFrom(Customer customer, Project project, String invoiceNumber, BigDecimal amount,
                           BigDecimal paidAmount, LocalDate issueDate, LocalDate dueDate, InvoiceStatus status) {
        this.customer = customer;
        this.project = project;
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public void updateAmountFromLines(BigDecimal amount) {
        this.amount = amount;
        if (this.paidAmount.compareTo(this.amount) > 0) {
            this.paidAmount = this.amount;
        }
        if (this.paidAmount.compareTo(this.amount) >= 0 && this.amount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InvoiceStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void registerCompletedPayment(BigDecimal paymentAmount) {
        this.paidAmount = this.paidAmount.add(paymentAmount);
        if (this.paidAmount.compareTo(this.amount) >= 0) {
            this.paidAmount = this.amount;
            this.status = InvoiceStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void removeCompletedPayment(BigDecimal paymentAmount) {
        this.paidAmount = this.paidAmount.subtract(paymentAmount);
        if (this.paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = InvoiceStatus.SENT;
        } else if (this.paidAmount.compareTo(this.amount) < 0) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public boolean isPaymentReminderAllowed() {
        return this.status != InvoiceStatus.PAID && this.status != InvoiceStatus.CANCELLED;
    }

    public void markPaymentReminderSent(LocalDateTime sentAt) {
        this.lastPaymentReminderAt = sentAt;
        this.paymentReminderCount++;
        if (this.dueDate != null && this.dueDate.isBefore(sentAt.toLocalDate())
                && this.status != InvoiceStatus.PAID && this.status != InvoiceStatus.CANCELLED) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }
}
