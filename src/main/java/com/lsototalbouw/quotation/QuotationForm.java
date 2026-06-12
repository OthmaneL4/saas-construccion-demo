package com.lsototalbouw.quotation;

import com.lsototalbouw.common.enums.QuotationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class QuotationForm {

    @NotNull(message = "Selecciona un cliente")
    private Long customerId;

    @NotBlank(message = "El numero de presupuesto es obligatorio")
    @Size(max = 40)
    private String quotationNumber;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Introduce el importe")
    @DecimalMin(value = "0.00", message = "El importe no puede ser negativo")
    private BigDecimal amount = BigDecimal.ZERO;

    private LocalDate issueDate = LocalDate.now();

    private LocalDate expiryDate = LocalDate.now().plusDays(30);

    @NotNull
    private QuotationStatus status = QuotationStatus.DRAFT;

    public static QuotationForm from(Quotation quotation) {
        QuotationForm form = new QuotationForm();
        form.setCustomerId(quotation.getCustomer().getId());
        form.setQuotationNumber(quotation.getQuotationNumber());
        form.setTitle(quotation.getTitle());
        form.setDescription(quotation.getDescription());
        form.setAmount(quotation.getAmount());
        form.setIssueDate(quotation.getIssueDate());
        form.setExpiryDate(quotation.getExpiryDate());
        form.setStatus(quotation.getStatus());
        return form;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public boolean isEmptyNewForm() {
        return customerId == null
                && (quotationNumber == null || quotationNumber.isBlank())
                && (title == null || title.isBlank())
                && (description == null || description.isBlank())
                && BigDecimal.ZERO.compareTo(amount == null ? BigDecimal.ZERO : amount) == 0;
    }
}
