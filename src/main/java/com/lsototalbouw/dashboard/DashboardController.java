package com.lsototalbouw.dashboard;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.expense.ExpenseRepository;
import com.lsototalbouw.notification.BusinessNotificationService;
import com.lsototalbouw.project.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProjectRepository projects;
    private final ExpenseRepository expenses;
    private final CompanyContextService companyContext;
    private final OperationalAlertService alerts;
    private final BusinessNotificationService notificationService;

    public DashboardController(DashboardService dashboardService, ProjectRepository projects,
                               ExpenseRepository expenses,
                               CompanyContextService companyContext, OperationalAlertService alerts,
                               BusinessNotificationService notificationService) {
        this.dashboardService = dashboardService;
        this.projects = projects;
        this.expenses = expenses;
        this.companyContext = companyContext;
        this.alerts = alerts;
        this.notificationService = notificationService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        Long companyId = companyContext.currentCompanyId();
        List<OperationalAlert> currentAlerts = alerts.currentAlerts();
        notificationService.syncFromAlerts(currentAlerts);
        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("projects", projects.findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(companyId));
        model.addAttribute("invoices", dashboardService.openInvoicesForCurrentCompany());
        model.addAttribute("expenses", expenses.findByCompanyAccountIdAndActiveTrueOrderByExpenseDateDesc(companyId));
        model.addAttribute("alerts", currentAlerts);
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount());
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/index";
    }
}
