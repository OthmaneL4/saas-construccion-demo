package com.lsototalbouw.quotation;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.enums.QuotationStatus;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.customer.Customer;
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

@Entity
@Table(name = "quotations",
        uniqueConstraints = @UniqueConstraint(name = "uk_quotations_company_number", columnNames = {"company_account_id", "quotation_number"}),
        indexes = {
                @Index(name = "idx_quotations_company_active_date", columnList = "company_account_id, active, issue_date"),
                @Index(name = "idx_quotations_company_status_active", columnList = "company_account_id, status, active"),
                @Index(name = "idx_quotations_customer", columnList = "customer_id")
        })
public class Quotation extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "quotation_number", nullable = false, length = 40)
    private String quotationNumber;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuotationStatus status = QuotationStatus.DRAFT;

    protected Quotation() {
    }

    public Quotation(CompanyAccount companyAccount, Customer customer, String quotationNumber, String title,
                     String description, BigDecimal amount, LocalDate issueDate, LocalDate expiryDate,
                     QuotationStatus status) {
        this.companyAccount = companyAccount;
        this.customer = customer;
        this.quotationNumber = quotationNumber;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public void updateFrom(Customer customer, String quotationNumber, String title, String description,
                           BigDecimal amount, LocalDate issueDate, LocalDate expiryDate, QuotationStatus status) {
        this.customer = customer;
        this.quotationNumber = quotationNumber;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public void updateAmountFromLines(BigDecimal amount) {
        this.amount = amount;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public void markAccepted() {
        this.status = QuotationStatus.ACCEPTED;
    }

    public void updateStatus(QuotationStatus status) {
        this.status = status;
    }
}
