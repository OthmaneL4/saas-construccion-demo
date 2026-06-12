package com.lsototalbouw.expense;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;

    public ExpenseService(ExpenseRepository expenses, ProjectRepository projects, CompanyContextService companyContext) {
        this.expenses = expenses;
        this.projects = projects;
        this.companyContext = companyContext;
    }

    @Transactional
    public Expense create(ExpenseForm form) {
        Long companyId = companyContext.currentCompanyId();
        Project project = null;
        if (form.getProjectId() != null) {
            project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        }

        Expense expense = new Expense(
                companyContext.currentCompany(),
                project,
                normalizeText(form.getDescription()),
                form.getAmount(),
                normalizeText(form.getCategory()),
                form.getExpenseDate()
        );
        return expenses.save(expense);
    }

    @Transactional(readOnly = true)
    public Expense getCurrentCompanyExpense(Long id) {
        return expenses.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado"));
    }

    @Transactional
    public Expense update(Long id, ExpenseForm form) {
        Long companyId = companyContext.currentCompanyId();
        Expense expense = getCurrentCompanyExpense(id);
        Project project = null;
        if (form.getProjectId() != null) {
            project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        }

        expense.updateFrom(
                project,
                normalizeText(form.getDescription()),
                form.getAmount(),
                normalizeText(form.getCategory()),
                form.getExpenseDate()
        );
        return expense;
    }

    @Transactional
    public void archive(Long id) {
        Expense expense = getCurrentCompanyExpense(id);
        expense.setActive(false);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
