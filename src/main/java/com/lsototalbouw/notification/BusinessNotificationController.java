package com.lsototalbouw.notification;

import com.lsototalbouw.dashboard.OperationalAlertService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BusinessNotificationController {

    private final BusinessNotificationService notificationService;
    private final OperationalAlertService alertService;

    public BusinessNotificationController(BusinessNotificationService notificationService,
                                          OperationalAlertService alertService) {
        this.notificationService = notificationService;
        this.alertService = alertService;
    }

    @GetMapping("/notifications")
    public String index(Model model) {
        notificationService.syncFromAlerts(alertService.currentAlerts());
        if (!model.containsAttribute("notificationForm")) {
            model.addAttribute("notificationForm", new BusinessNotificationForm());
        }
        model.addAttribute("notifications", notificationService.listCurrentCompany());
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount());
        model.addAttribute("pageTitle", "Notificaciones");
        return "notifications/index";
    }

    @PostMapping("/notifications")
    public String create(@Valid @ModelAttribute("notificationForm") BusinessNotificationForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            notificationService.syncFromAlerts(alertService.currentAlerts());
            model.addAttribute("notifications", notificationService.listCurrentCompany());
            model.addAttribute("notificationUnreadCount", notificationService.unreadCount());
            model.addAttribute("pageTitle", "Notificaciones");
            return "notifications/index";
        }
        notificationService.createManualReminder(form);
        redirectAttributes.addFlashAttribute("success", "Recordatorio creado correctamente.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean updated = notificationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("success", updated
                    ? "Notificacion marcada como leida."
                    : "La notificacion ya estaba leida.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead();
        redirectAttributes.addFlashAttribute("success", "Notificaciones actualizadas.");
        return "redirect:/notifications";
    }
}
