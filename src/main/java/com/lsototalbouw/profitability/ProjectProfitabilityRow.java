package com.lsototalbouw.profitability;

import com.lsototalbouw.project.Project;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProjectProfitabilityRow {

    private final Project project;
    private final BigDecimal budget;
    private final BigDecimal invoiced;
    private final BigDecimal paid;
    private final BigDecimal expenses;
    private final BigDecimal hours;
    private final BigDecimal laborValue;

    public ProjectProfitabilityRow(Project project, BigDecimal budget, BigDecimal invoiced, BigDecimal paid,
                                   BigDecimal expenses, BigDecimal hours, BigDecimal laborValue) {
        this.project = project;
        this.budget = safe(budget);
        this.invoiced = safe(invoiced);
        this.paid = safe(paid);
        this.expenses = safe(expenses);
        this.hours = safe(hours);
        this.laborValue = safe(laborValue);
    }

    public Project getProject() {
        return project;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getInvoiced() {
        return invoiced;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public BigDecimal getLaborValue() {
        return laborValue;
    }

    public BigDecimal getEstimatedCost() {
        return expenses.add(laborValue);
    }

    public BigDecimal getMargin() {
        return invoiced.subtract(getEstimatedCost());
    }

    public BigDecimal getCashMargin() {
        return paid.subtract(getEstimatedCost());
    }

    public BigDecimal getBudgetUsagePercent() {
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getEstimatedCost().multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);
    }

    public String getHealthLabel() {
        if (getMargin().compareTo(BigDecimal.ZERO) >= 0) {
            return "Rentable";
        }
        if (getCashMargin().compareTo(BigDecimal.ZERO) < 0) {
            return "Riesgo";
        }
        return "Vigilar";
    }

    public String getHealthClass() {
        if ("Rentable".equals(getHealthLabel())) {
            return "status-PAID";
        }
        if ("Riesgo".equals(getHealthLabel())) {
            return "status-OVERDUE";
        }
        return "status-SENT";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
