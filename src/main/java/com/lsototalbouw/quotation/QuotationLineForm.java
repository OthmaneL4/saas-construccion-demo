package com.lsototalbouw.quotation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class QuotationLineForm {

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 240)
    private String description;

    @NotNull(message = "Introduce la cantidad")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity = BigDecimal.ONE;

    @NotNull(message = "Introduce el precio unitario")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @NotNull(message = "Introduce el IVA")
    @DecimalMin(value = "0.00", message = "El IVA no puede ser negativo")
    private BigDecimal vatRate = BigDecimal.valueOf(21);

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public static QuotationLineForm from(QuotationLine line) {
        QuotationLineForm form = new QuotationLineForm();
        form.setDescription(line.getDescription());
        form.setQuantity(line.getQuantity());
        form.setUnitPrice(line.getUnitPrice());
        form.setVatRate(line.getVatRate());
        return form;
    }
}
