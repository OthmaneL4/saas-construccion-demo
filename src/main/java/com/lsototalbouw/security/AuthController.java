package com.lsototalbouw.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spring MVC controller managing the user authentication login page.
 *
 * <p>Serves the login interface and configures demo credentials visibility based on application properties.
 */
@Controller
public class AuthController {

    /**
     * Renders the login screen.
     *
     * <p>Inspects demo login configuration to append safe public-demo credentials to the UI model.
     * If enabled, pre-configured credentials are displayed to the user for quick demo access.
     *
     * @param model            the UI MVC model
     * @param demoLoginEnabled the configuration flag indicating if demo login helpers should be displayed
     * @return the logical view template name {@code "auth/login"}
     */
    @GetMapping("/login")
    public String login(Model model,
                        @Value("${app.demo-login.enabled:false}") boolean demoLoginEnabled,
                        @Value("${app.demo-login.username:demo@lsototalbouw.nl}") String demoUsername,
                        @Value("${app.demo-login.password:Demo2026!}") String demoPassword,
                        @Value("${app.demo-login.note:Demo environment with fictional data.}") String demoNote) {
        model.addAttribute("demoLoginEnabled", demoLoginEnabled);
        model.addAttribute("demoUsername", demoUsername);
        model.addAttribute("demoPassword", demoPassword);
        model.addAttribute("demoNote", demoNote);
        return "auth/login";
    }
}
