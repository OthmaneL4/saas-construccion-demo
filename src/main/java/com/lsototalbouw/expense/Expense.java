package com.lsototalbouw.expense;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expenses_company_active_date", columnList = "company_account_id, active, expense_date"),
        @Index(name = "idx_expenses_project", columnList = "project_id")
})
public class Expense extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 80)
    private String category;

    private LocalDate expenseDate;

    protected Expense() {
    }

    public Expense(CompanyAccount companyAccount, Project project, String description, BigDecimal amount,
                   String category, LocalDate expenseDate) {
        this.companyAccount = companyAccount;
        this.project = project;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
    }

    public Project getProject() {
        return project;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public void updateFrom(Project project, String description, BigDecimal amount, String category, LocalDate expenseDate) {
        this.project = project;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
    }
}
