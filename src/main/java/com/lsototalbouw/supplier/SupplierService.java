package com.lsototalbouw.supplier;

import com.lsototalbouw.company.CompanyContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final CompanyContextService companyContext;

    public SupplierService(SupplierRepository suppliers, CompanyContextService companyContext) {
        this.suppliers = suppliers;
        this.companyContext = companyContext;
    }

    @Transactional(readOnly = true)
    public Supplier getCurrentCompanySupplier(Long id) {
        return suppliers.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    }

    @Transactional
    public Supplier create(SupplierForm form) {
        Supplier supplier = new Supplier(
                companyContext.currentCompany(),
                form.getName().trim(),
                clean(form.getContactName()),
                clean(form.getEmail()),
                clean(form.getPhone()),
                clean(form.getAddress()),
                clean(form.getCity())
        );
        return suppliers.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, SupplierForm form) {
        Supplier supplier = getCurrentCompanySupplier(id);
        supplier.updateFrom(
                form.getName().trim(),
                clean(form.getContactName()),
                clean(form.getEmail()),
                clean(form.getPhone()),
                clean(form.getAddress()),
                clean(form.getCity())
        );
        return supplier;
    }

    @Transactional
    public void archive(Long id) {
        Supplier supplier = getCurrentCompanySupplier(id);
        supplier.setActive(false);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
