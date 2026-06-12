package com.lsototalbouw.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerForm {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 160)
    private String name;

    @Email(message = "Introduce un email valido")
    @Size(max = 160)
    private String email;

    @Size(max = 80)
    private String phone;

    @Size(max = 240)
    private String address;

    @Size(max = 80)
    private String city;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public static CustomerForm from(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.setName(customer.getName());
        form.setEmail(customer.getEmail());
        form.setPhone(customer.getPhone());
        form.setAddress(customer.getAddress());
        form.setCity(customer.getCity());
        return form;
    }
}
