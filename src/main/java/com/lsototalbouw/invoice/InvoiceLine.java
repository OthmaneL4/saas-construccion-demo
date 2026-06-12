package com.lsototalbouw.invoice;

import com.lsototalbouw.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "invoice_lines", indexes = {
        @Index(name = "idx_invoice_lines_invoice_active", columnList = "invoice_id, active")
})
public class InvoiceLine extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.valueOf(21);

    protected InvoiceLine() {
    }

    public InvoiceLine(Invoice invoice, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
        this.invoice = invoice;
        this.description = description;
        this.quantity = normalize(quantity);
        this.unitPrice = normalize(unitPrice);
        this.vatRate = normalize(vatRate);
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getSubtotal() {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getVatAmount() {
        return getSubtotal().multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getVatAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    public void updateFrom(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
        this.description = description;
        this.quantity = normalize(quantity);
        this.unitPrice = normalize(unitPrice);
        this.vatRate = normalize(vatRate);
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
