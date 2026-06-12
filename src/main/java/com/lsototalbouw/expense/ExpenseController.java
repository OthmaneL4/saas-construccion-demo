package com.lsototalbouw.expense;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.project.ProjectRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExpenseController {

    private final ExpenseRepository expenses;
    private final ExpenseService expenseService;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public ExpenseController(ExpenseRepository expenses, ExpenseService expenseService, ProjectRepository projects,
                             CompanyContextService companyContext, AuditService auditService) {
        this.expenses = expenses;
        this.expenseService = expenseService;
        this.projects = projects;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/expenses")
    public String index(Model model, @ModelAttribute("expenseForm") ExpenseForm expenseForm,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "projectId", required = false) Long projectId,
                        @RequestParam(name = "category", required = false) String category,
                        @RequestParam(name = "from", required = false) LocalDate fromDate,
                        @RequestParam(name = "to", required = false) LocalDate toDate) {
        Long companyId = companyContext.currentCompanyId();
        addIndexData(model, query, projectId, category, fromDate, toDate);
        model.addAttribute("expenseForm", expenseForm);
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        return "expenses/index";
    }

    @PostMapping("/expenses")
    public String create(@Valid @ModelAttribute("expenseForm") ExpenseForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Long companyId = companyContext.currentCompanyId();
            addIndexData(model, null, null, null, null, null);
            model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
            return "expenses/index";
        }
        Expense expense = expenseService.create(form);
        auditService.record(AuditAction.CREATE, "Gastos", expense.getId(),
                "Gasto creado: " + expense.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Gasto registrado correctamente.");
        return "redirect:/expenses";
    }

    @GetMapping("/expenses/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("pageTitle", "Gasto");
        model.addAttribute("expense", expenseService.getCurrentCompanyExpense(id));
        return "expenses/detail";
    }

    @GetMapping("/expenses/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        Long companyId = companyContext.currentCompanyId();
        Expense expense = expenseService.getCurrentCompanyExpense(id);
        model.addAttribute("pageTitle", "Editar gasto");
        model.addAttribute("expense", expense);
        model.addAttribute("expenseForm", ExpenseForm.from(expense));
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("expenseCategories", expenses.findCategoriesByCompanyId(companyId));
        return "expenses/edit";
    }

    @PostMapping("/expenses/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("expenseForm") ExpenseForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long companyId = companyContext.currentCompanyId();
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Editar gasto");
            model.addAttribute("expense", expenseService.getCurrentCompanyExpense(id));
            model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
            model.addAttribute("expenseCategories", expenses.findCategoriesByCompanyId(companyId));
            return "expenses/edit";
        }
        Expense expense = expenseService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Gastos", id,
                "Gasto actualizado: " + expense.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Gasto actualizado correctamente.");
        return "redirect:/expenses/{id}";
    }

    @PostMapping("/expenses/{id}/archive")
    public String archive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Expense expense = expenseService.getCurrentCompanyExpense(id);
        expenseService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Gastos", id,
                "Gasto archivado: " + expense.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Gasto archivado correctamente.");
        return "redirect:/expenses";
    }

    private void addIndexData(Model model, String query, Long projectId, String category,
                              LocalDate fromDate, LocalDate toDate) {
        Long companyId = companyContext.currentCompanyId();
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        String cleanCategory = category == null || category.isBlank() ? null : category.trim();
        List<Expense> filteredExpenses = cleanQuery == null && projectId == null && cleanCategory == null
                && fromDate == null && toDate == null
                ? expenses.findByCompanyAccountIdAndActiveTrueOrderByExpenseDateDesc(companyId)
                : expenses.searchByCompanyId(companyId, cleanQuery, projectId, cleanCategory, fromDate, toDate);
        BigDecimal totalAmount = filteredExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageAmount = filteredExpenses.isEmpty()
                ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(filteredExpenses.size()), 2, RoundingMode.HALF_UP);
        model.addAttribute("pageTitle", "Gastos");
        model.addAttribute("expenses", filteredExpenses);
        model.addAttribute("expenseCount", filteredExpenses.size());
        model.addAttribute("totalFilteredExpenses", totalAmount);
        model.addAttribute("averageFilteredExpense", averageAmount);
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedProjectId", projectId);
        model.addAttribute("selectedCategory", cleanCategory);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("expenseCategories", expenses.findCategoriesByCompanyId(companyId));
    }
}
