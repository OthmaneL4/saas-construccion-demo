package com.lsototalbouw.receivable;

import com.lsototalbouw.notification.BusinessNotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReceivablesController {

    private final ReceivablesService receivablesService;
    private final BusinessNotificationService notificationService;

    public ReceivablesController(ReceivablesService receivablesService,
                                 BusinessNotificationService notificationService) {
        this.receivablesService = receivablesService;
        this.notificationService = notificationService;
    }

    @GetMapping("/receivables")
    public String index(Model model,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "priority", required = false) String priority) {
        ReceivablesView receivables = receivablesService.currentCompanyReceivables(query, priority);
        model.addAttribute("pageTitle", "Cobros pendientes");
        model.addAttribute("receivables", receivables);
        model.addAttribute("summary", receivables.summary());
        model.addAttribute("invoiceRows", receivables.invoices());
        model.addAttribute("customerRows", receivables.customers());
        model.addAttribute("query", query == null || query.isBlank() ? null : query.trim());
        model.addAttribute("selectedPriority", receivablesService.cleanPriority(priority));
        model.addAttribute("priorityOptions", ReceivablesService.PRIORITY_OPTIONS);
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount());
        return "receivables/index";
    }
}
