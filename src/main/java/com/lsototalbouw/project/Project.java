package com.lsototalbouw.project;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.enums.ProjectStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_projects_company_active_start", columnList = "company_account_id, active, start_date"),
        @Index(name = "idx_projects_company_status_active", columnList = "company_account_id, status, active"),
        @Index(name = "idx_projects_customer", columnList = "customer_id")
})
public class Project extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 240)
    private String workAddress;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProjectStatus status = ProjectStatus.PLANNED;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget = BigDecimal.ZERO;

    protected Project() {
    }

    public Project(CompanyAccount companyAccount, Customer customer, String name, String workAddress,
                   LocalDate startDate, ProjectStatus status, BigDecimal budget) {
        this.companyAccount = companyAccount;
        this.customer = customer;
        this.name = name;
        this.workAddress = workAddress;
        this.startDate = startDate;
        this.status = status;
        this.budget = budget;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getName() {
        return name;
    }

    public String getWorkAddress() {
        return workAddress;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public void updateFrom(Customer customer, String name, String workAddress, LocalDate startDate,
                           ProjectStatus status, BigDecimal budget) {
        this.customer = customer;
        this.name = name;
        this.workAddress = workAddress;
        this.startDate = startDate;
        this.status = status;
        this.budget = budget;
    }
}
