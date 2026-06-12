package com.lsototalbouw.customer;

import com.lsototalbouw.company.CompanyContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final CompanyContextService companyContext;

    public CustomerService(CustomerRepository customers, CompanyContextService companyContext) {
        this.customers = customers;
        this.companyContext = companyContext;
    }

    @Transactional
    public Customer create(CustomerForm form) {
        Customer customer = new Customer(
                companyContext.currentCompany(),
                form.getName().trim(),
                clean(form.getEmail()),
                clean(form.getPhone()),
                clean(form.getAddress()),
                clean(form.getCity())
        );
        return customers.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCurrentCompanyCustomer(Long id) {
        return customers.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    @Transactional
    public Customer update(Long id, CustomerForm form) {
        Customer customer = getCurrentCompanyCustomer(id);
        customer.updateFrom(
                form.getName().trim(),
                clean(form.getEmail()),
                clean(form.getPhone()),
                clean(form.getAddress()),
                clean(form.getCity())
        );
        return customer;
    }

    @Transactional
    public void archive(Long id) {
        Customer customer = getCurrentCompanyCustomer(id);
        customer.setActive(false);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
