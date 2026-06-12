package com.lsototalbouw.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierForm {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 160)
    private String name;

    @Size(max = 120)
    private String contactName;

    @Email(message = "Introduce un email valido")
    @Size(max = 160)
    private String email;

    @Size(max = 80)
    private String phone;

    @Size(max = 240)
    private String address;

    @Size(max = 80)
    private String city;

    public static SupplierForm from(Supplier supplier) {
        SupplierForm form = new SupplierForm();
        form.setName(supplier.getName());
        form.setContactName(supplier.getContactName());
        form.setEmail(supplier.getEmail());
        form.setPhone(supplier.getPhone());
        form.setAddress(supplier.getAddress());
        form.setCity(supplier.getCity());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
