package com.lsototalbouw.profitability;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfitabilityController {

    private final ProfitabilityService profitabilityService;

    public ProfitabilityController(ProfitabilityService profitabilityService) {
        this.profitabilityService = profitabilityService;
    }

    @GetMapping("/profitability")
    public String index(Model model,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "health", required = false) String health) {
        model.addAttribute("pageTitle", "Rentabilidad");
        model.addAttribute("summary", profitabilityService.currentCompanySummary(query, health));
        model.addAttribute("query", query == null || query.isBlank() ? null : query.trim());
        model.addAttribute("selectedHealth", health == null || health.isBlank() ? null : health.trim());
        model.addAttribute("healthOptions", java.util.List.of("Rentable", "Vigilar", "Riesgo"));
        return "profitability/index";
    }
}
