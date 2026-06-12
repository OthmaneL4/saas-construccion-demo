package com.lsototalbouw.supplier;

public class SupplierDirectorySummary {

    private final long totalSuppliers;
    private final long suppliersWithEmail;
    private final long suppliersWithPhone;
    private final long coveredCities;

    public SupplierDirectorySummary(long totalSuppliers, long suppliersWithEmail, long suppliersWithPhone,
                                    long coveredCities) {
        this.totalSuppliers = totalSuppliers;
        this.suppliersWithEmail = suppliersWithEmail;
        this.suppliersWithPhone = suppliersWithPhone;
        this.coveredCities = coveredCities;
    }

    public long getTotalSuppliers() {
        return totalSuppliers;
    }

    public long getSuppliersWithEmail() {
        return suppliersWithEmail;
    }

    public long getSuppliersWithPhone() {
        return suppliersWithPhone;
    }

    public long getCoveredCities() {
        return coveredCities;
    }
}
