package com.lsototalbouw.invoice;

import com.lsototalbouw.common.enums.InvoiceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceForm {

    @NotNull(message = "Selecciona un cliente")
    private Long customerId;

    private Long projectId;

    @NotBlank(message = "El numero de factura es obligatorio")
    @Size(max = 40)
    private String invoiceNumber;

    @NotNull(message = "Introduce el importe")
    @DecimalMin(value = "0.00", message = "El importe no puede ser negativo")
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull(message = "Introduce el importe cobrado")
    @DecimalMin(value = "0.00", message = "El importe cobrado no puede ser negativo")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    private LocalDate issueDate = LocalDate.now();

    private LocalDate dueDate;

    @NotNull
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public static InvoiceForm from(Invoice invoice) {
        InvoiceForm form = new InvoiceForm();
        form.setCustomerId(invoice.getCustomer().getId());
        form.setProjectId(invoice.getProject() != null ? invoice.getProject().getId() : null);
        form.setInvoiceNumber(invoice.getInvoiceNumber());
        form.setAmount(invoice.getAmount());
        form.setPaidAmount(invoice.getPaidAmount());
        form.setIssueDate(invoice.getIssueDate());
        form.setDueDate(invoice.getDueDate());
        form.setStatus(invoice.getStatus());
        return form;
    }

    public boolean isEmptyNewForm() {
        return customerId == null
                && projectId == null
                && (invoiceNumber == null || invoiceNumber.isBlank())
                && BigDecimal.ZERO.compareTo(amount == null ? BigDecimal.ZERO : amount) == 0
                && BigDecimal.ZERO.compareTo(paidAmount == null ? BigDecimal.ZERO : paidAmount) == 0;
    }
}
