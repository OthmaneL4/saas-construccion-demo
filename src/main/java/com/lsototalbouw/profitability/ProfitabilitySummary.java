package com.lsototalbouw.profitability;

import java.math.BigDecimal;
import java.util.List;

public class ProfitabilitySummary {

    private final List<ProjectProfitabilityRow> rows;

    public ProfitabilitySummary(List<ProjectProfitabilityRow> rows) {
        this.rows = rows;
    }

    public List<ProjectProfitabilityRow> getRows() {
        return rows;
    }

    public BigDecimal getTotalBudget() {
        return sum(ProjectProfitabilityRow::getBudget);
    }

    public BigDecimal getTotalInvoiced() {
        return sum(ProjectProfitabilityRow::getInvoiced);
    }

    public BigDecimal getTotalPaid() {
        return sum(ProjectProfitabilityRow::getPaid);
    }

    public BigDecimal getTotalEstimatedCost() {
        return sum(ProjectProfitabilityRow::getEstimatedCost);
    }

    public BigDecimal getTotalMargin() {
        return sum(ProjectProfitabilityRow::getMargin);
    }

    private BigDecimal sum(java.util.function.Function<ProjectProfitabilityRow, BigDecimal> mapper) {
        return rows.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
