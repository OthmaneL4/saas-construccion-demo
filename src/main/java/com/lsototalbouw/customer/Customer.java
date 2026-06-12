package com.lsototalbouw.customer;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_company_active_name", columnList = "company_account_id, active, name")
})
public class Customer extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String name;

    @Email
    @Column(length = 160)
    private String email;

    @Column(length = 80)
    private String phone;

    @Column(length = 240)
    private String address;

    @Column(length = 80)
    private String city;

    protected Customer() {
    }

    public Customer(CompanyAccount companyAccount, String name, String email, String phone, String address, String city) {
        this.companyAccount = companyAccount;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public void updateFrom(String name, String email, String phone, String address, String city) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
    }
}
