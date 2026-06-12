package com.lsototalbouw.profitability;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.invoice.InvoiceRepository;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import com.lsototalbouw.timesheet.WorkLogRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds profitability analytics for projects in the current company.
 *
 * <p>The service aggregates budget, invoiced revenue, paid revenue, expenses, billable hours, and labor value
 * from repository-level totals. Rows are returned in ascending margin order so financial risk appears first.
 */
@Service
public class ProfitabilityService {

    private final CompanyContextService companyContext;
    private final ProjectRepository projects;
    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;
    private final WorkLogRepository workLogs;

    public ProfitabilityService(CompanyContextService companyContext, ProjectRepository projects,
                                InvoiceRepository invoices, ExpenseRepository expenses,
                                WorkLogRepository workLogs) {
        this.companyContext = companyContext;
        this.projects = projects;
        this.invoices = invoices;
        this.expenses = expenses;
        this.workLogs = workLogs;
    }

    /**
     * Returns the full profitability summary for the current company.
     *
     * @return unfiltered profitability summary
     */
    @Transactional(readOnly = true)
    public ProfitabilitySummary currentCompanySummary() {
        return currentCompanySummary(null, null);
    }

    /**
     * Returns a filtered profitability summary for the current company.
     *
     * @param query  optional project/customer search text
     * @param health optional health label filter
     * @return filtered profitability summary ordered by margin risk
     */
    @Transactional(readOnly = true)
    public ProfitabilitySummary currentCompanySummary(String query, String health) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        String cleanHealth = health == null || health.isBlank() ? null : health.trim();
        List<ProjectProfitabilityRow> rows = projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId)
                .stream()
                .map(project -> rowFor(companyId, project))
                .filter(row -> matchesQuery(row, cleanQuery))
                .filter(row -> matchesHealth(row, cleanHealth))
                .sorted(Comparator.comparing(ProjectProfitabilityRow::getMargin))
                .toList();
        return new ProfitabilitySummary(rows);
    }

    /**
     * Builds a profitability row for a single project in the current company.
     *
     * @param projectId project identifier
     * @return project profitability row with aggregated financial metrics
     * @throws IllegalArgumentException when the project is missing, archived, or belongs to another company
     */
    @Transactional(readOnly = true)
    public ProjectProfitabilityRow currentCompanyProjectRow(Long projectId) {
        Long companyId = companyContext.currentCompanyId();
        Project project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        return rowFor(companyId, project);
    }

    private ProjectProfitabilityRow rowFor(Long companyId, Project project) {
        Long projectId = project.getId();
        return new ProjectProfitabilityRow(
                project,
                project.getBudget(),
                invoices.totalInvoicedByCompanyIdAndProjectId(companyId, projectId),
                invoices.totalPaidByCompanyIdAndProjectId(companyId, projectId),
                expenses.totalExpensesByCompanyIdAndProjectId(companyId, projectId),
                workLogs.totalHoursByCompanyIdAndProjectId(companyId, projectId),
                workLogs.totalLaborValueByCompanyIdAndProjectId(companyId, projectId)
        );
    }

    private boolean matchesQuery(ProjectProfitabilityRow row, String query) {
        if (query == null) {
            return true;
        }
        Project project = row.getProject();
        String projectName = project.getName() == null ? "" : project.getName();
        String customerName = project.getCustomer() == null || project.getCustomer().getName() == null
                ? "" : project.getCustomer().getName();
        return projectName.toLowerCase(Locale.ROOT).contains(query)
                || customerName.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesHealth(ProjectProfitabilityRow row, String health) {
        return health == null || row.getHealthLabel().equals(health);
    }
}
