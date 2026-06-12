package com.lsototalbouw.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseForm {

    private Long projectId;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 160)
    private String description;

    @NotNull(message = "Introduce un importe")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
    private BigDecimal amount;

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 80)
    private String category;

    private LocalDate expenseDate = LocalDate.now();

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public static ExpenseForm from(Expense expense) {
        ExpenseForm form = new ExpenseForm();
        form.setProjectId(expense.getProject() != null ? expense.getProject().getId() : null);
        form.setDescription(expense.getDescription());
        form.setAmount(expense.getAmount());
        form.setCategory(expense.getCategory());
        form.setExpenseDate(expense.getExpenseDate());
        return form;
    }
}
