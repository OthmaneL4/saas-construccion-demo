package com.lsototalbouw.material;

import java.math.BigDecimal;
import java.util.List;

public class MaterialInventorySummary {

    private final int totalMaterials;
    private final int lowStockMaterials;
    private final int totalStockUnits;
    private final BigDecimal inventoryValue;
    private final BigDecimal reorderCost;

    private MaterialInventorySummary(int totalMaterials, int lowStockMaterials, int totalStockUnits,
                                     BigDecimal inventoryValue, BigDecimal reorderCost) {
        this.totalMaterials = totalMaterials;
        this.lowStockMaterials = lowStockMaterials;
        this.totalStockUnits = totalStockUnits;
        this.inventoryValue = inventoryValue;
        this.reorderCost = reorderCost;
    }

    public static MaterialInventorySummary from(List<MaterialItem> materials) {
        int totalMaterials = materials.size();
        int lowStockMaterials = 0;
        int totalStockUnits = 0;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        BigDecimal reorderCost = BigDecimal.ZERO;

        for (MaterialItem material : materials) {
            if (material.isLowStock()) {
                lowStockMaterials++;
            }
            totalStockUnits += material.getStockQuantity();
            inventoryValue = inventoryValue.add(material.getUnitCost()
                    .multiply(BigDecimal.valueOf(material.getStockQuantity())));

            int reorderUnits = Math.max(material.getMinimumStock() - material.getStockQuantity(), 0);
            reorderCost = reorderCost.add(material.getUnitCost().multiply(BigDecimal.valueOf(reorderUnits)));
        }

        return new MaterialInventorySummary(totalMaterials, lowStockMaterials, totalStockUnits,
                inventoryValue, reorderCost);
    }

    public int getTotalMaterials() {
        return totalMaterials;
    }

    public int getLowStockMaterials() {
        return lowStockMaterials;
    }

    public int getTotalStockUnits() {
        return totalStockUnits;
    }

    public BigDecimal getInventoryValue() {
        return inventoryValue;
    }

    public BigDecimal getReorderCost() {
        return reorderCost;
    }
}
