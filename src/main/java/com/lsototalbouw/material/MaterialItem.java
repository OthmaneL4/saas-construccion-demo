package com.lsototalbouw.material;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "materials", indexes = {
        @Index(name = "idx_materials_company_active_name", columnList = "company_account_id, active, name")
})
public class MaterialItem extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    private int minimumStock;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    protected MaterialItem() {
    }

    public MaterialItem(CompanyAccount companyAccount, String name, String unit, int stockQuantity,
                        int minimumStock, BigDecimal unitCost) {
        this.companyAccount = companyAccount;
        this.name = name;
        this.unit = unit;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
        this.unitCost = unitCost;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void updateFrom(String name, String unit, int stockQuantity, int minimumStock, BigDecimal unitCost) {
        this.name = name;
        this.unit = unit;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
        this.unitCost = unitCost;
    }

    public boolean isLowStock() {
        return stockQuantity <= minimumStock;
    }
}
