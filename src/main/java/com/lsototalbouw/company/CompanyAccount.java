package com.lsototalbouw.company;

import com.lsototalbouw.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Domain entity representing a tenant company account.
 *
 * <p>Stores business-level profile settings, legal identifiers (VAT, KvK), contact details,
 * and financial rules (IBAN, payment terms) which apply globally to all operations linked to this tenant.
 */
@Entity
@Table(name = "company_accounts")
public class CompanyAccount extends BaseEntity {

    /** The legal or commercial name of the company. */
    @Column(nullable = false, length = 160)
    private String name;

    /** The Value Added Tax (VAT) registration number of the company. */
    @Column(length = 80)
    private String vatNumber;

    /** The primary contact email address of the company. */
    @Column(length = 160)
    private String email;

    /** The main telephone contact number of the company. */
    @Column(length = 80)
    private String phone;

    /** The physical head office address of the company. */
    @Column(length = 240)
    private String address;

    /** The Chamber of Commerce registration number (Kamer van Koophandel) in the Netherlands. */
    @Column(length = 80)
    private String kvkNumber;

    /** The International Bank Account Number (IBAN) used for client billing payouts. */
    @Column(length = 80)
    private String iban;

    /** The default number of net days allowed for invoice payments. Defaults to {@code 14} days. */
    @Column(nullable = false)
    private int paymentTermsDays = 14;

    /**
     * Protected no-arg constructor required by JPA.
     */
    public CompanyAccount() {
    }

    /**
     * Initializes a new company account with essential profile details.
     *
     * @param name      the company name
     * @param vatNumber the VAT number
     * @param email     the contact email
     * @param phone     the contact phone number
     * @param address   the company address
     */
    public CompanyAccount(String name, String vatNumber, String email, String phone, String address) {
        this.name = name;
        this.vatNumber = vatNumber;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getVatNumber() {
        return vatNumber;
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

    public String getKvkNumber() {
        return kvkNumber;
    }

    public String getIban() {
        return iban;
    }

    public int getPaymentTermsDays() {
        return paymentTermsDays;
    }

    /**
     * Updates all profile settings and billing configuration variables in a single operation.
     *
     * @param name             the updated company name
     * @param vatNumber        the updated VAT identification number
     * @param email            the updated contact email address
     * @param phone            the updated contact telephone number
     * @param address          the updated physical office address
     * @param kvkNumber        the updated Chamber of Commerce registration number (KvK)
     * @param iban             the updated corporate IBAN
     * @param paymentTermsDays the updated default payment terms duration in days
     */
    public void updateFrom(String name, String vatNumber, String email, String phone, String address,
                           String kvkNumber, String iban, int paymentTermsDays) {
        this.name = name;
        this.vatNumber = vatNumber;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.kvkNumber = kvkNumber;
        this.iban = iban;
        this.paymentTermsDays = paymentTermsDays;
    }
}
