package com.lsototalbouw.material;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class MaterialForm {

    @NotBlank(message = "El nombre del material es obligatorio")
    @Size(max = 160)
    private String name;

    @NotBlank(message = "La unidad es obligatoria")
    @Size(max = 40)
    private String unit;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private int stockQuantity;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    private int minimumStock;

    @NotNull(message = "Introduce el coste unitario")
    @DecimalMin(value = "0.00", message = "El coste no puede ser negativo")
    private BigDecimal unitCost = BigDecimal.ZERO;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public static MaterialForm from(MaterialItem material) {
        MaterialForm form = new MaterialForm();
        form.setName(material.getName());
        form.setUnit(material.getUnit());
        form.setStockQuantity(material.getStockQuantity());
        form.setMinimumStock(material.getMinimumStock());
        form.setUnitCost(material.getUnitCost());
        return form;
    }
}
