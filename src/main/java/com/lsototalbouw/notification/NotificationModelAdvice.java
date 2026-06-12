package com.lsototalbouw.notification;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationModelAdvice {

    private final BusinessNotificationService notificationService;

    public NotificationModelAdvice(BusinessNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addNotificationCount(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            model.addAttribute("notificationUnreadCount", 0);
            return;
        }
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount());
    }
}
