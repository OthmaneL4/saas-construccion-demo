package com.lsototalbouw.supplier;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_company_active_name", columnList = "company_account_id, active, name")
})
public class Supplier extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 120)
    private String contactName;

    @Column(length = 160)
    private String email;

    @Column(length = 80)
    private String phone;

    @Column(length = 240)
    private String address;

    @Column(length = 80)
    private String city;

    protected Supplier() {
    }

    public Supplier(CompanyAccount companyAccount, String name, String contactName, String email,
                    String phone, String address, String city) {
        this.companyAccount = companyAccount;
        this.name = name;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
    }

    public void updateFrom(String name, String contactName, String email, String phone, String address, String city) {
        this.name = name;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    public String getName() {
        return name;
    }

    public String getContactName() {
        return contactName;
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
}
