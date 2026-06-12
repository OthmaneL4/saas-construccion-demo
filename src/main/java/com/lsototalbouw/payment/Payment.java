package com.lsototalbouw.payment;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.enums.PaymentStatus;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.invoice.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_company_active_date", columnList = "company_account_id, active, payment_date"),
        @Index(name = "idx_payments_invoice", columnList = "invoice_id")
})
public class Payment extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 80)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    protected Payment() {
    }

    public Payment(CompanyAccount companyAccount, Invoice invoice, BigDecimal amount, LocalDate paymentDate,
                   String method, PaymentStatus status) {
        this.companyAccount = companyAccount;
        this.invoice = invoice;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.status = status;
    }

    public void updateFrom(Invoice invoice, BigDecimal amount, LocalDate paymentDate, String method, PaymentStatus status) {
        this.invoice = invoice;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.status = status;
    }

    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
