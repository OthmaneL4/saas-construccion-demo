package com.lsototalbouw.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) represent the web form settings for editing a company account profile.
 *
 * <p>Enforces validation rules (length, format constraints) on user inputs before they are persisted
 * to the {@link CompanyAccount} domain model.
 */
public class CompanySettingsForm {

    @NotBlank
    @Size(max = 160)
    private String name;

    @Size(max = 80)
    private String vatNumber;

    @Email
    @Size(max = 160)
    private String email;

    @Size(max = 80)
    private String phone;

    @Size(max = 240)
    private String address;

    @Size(max = 80)
    private String kvkNumber;

    @Size(max = 80)
    private String iban;

    @Min(0)
    @Max(120)
    private int paymentTermsDays = 14;

    /**
     * Factory mapping method that constructs and populates a new form instance
     * using values from a {@link CompanyAccount} persistent entity.
     *
     * @param company the source company account entity
     * @return a populated {@link CompanySettingsForm} instance
     */
    public static CompanySettingsForm from(CompanyAccount company) {
        CompanySettingsForm form = new CompanySettingsForm();
        form.setName(company.getName());
        form.setVatNumber(company.getVatNumber());
        form.setEmail(company.getEmail());
        form.setPhone(company.getPhone());
        form.setAddress(company.getAddress());
        form.setKvkNumber(company.getKvkNumber());
        form.setIban(company.getIban());
        form.setPaymentTermsDays(company.getPaymentTermsDays());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getKvkNumber() {
        return kvkNumber;
    }

    public void setKvkNumber(String kvkNumber) {
        this.kvkNumber = kvkNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public int getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public void setPaymentTermsDays(int paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }
}
