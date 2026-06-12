package com.lsototalbouw.payment;

import com.lsototalbouw.common.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentForm {

    @NotNull(message = "Selecciona una factura")
    private Long invoiceId;

    @NotNull(message = "Introduce el importe")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
    private BigDecimal amount;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate paymentDate = LocalDate.now();

    @NotBlank(message = "El metodo de pago es obligatorio")
    @Size(max = 80)
    private String method = "Transferencia";

    @NotNull
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @Size(max = 240)
    private String returnUrl;

    public static PaymentForm from(Payment payment) {
        PaymentForm form = new PaymentForm();
        form.setInvoiceId(payment.getInvoice().getId());
        form.setAmount(payment.getAmount());
        form.setPaymentDate(payment.getPaymentDate());
        form.setMethod(payment.getMethod());
        form.setStatus(payment.getStatus());
        return form;
    }

    public boolean isEmptyNewForm() {
        return invoiceId == null
                && amount == null
                && returnUrl == null
                && PaymentStatus.COMPLETED == status
                && "Transferencia".equals(method);
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
