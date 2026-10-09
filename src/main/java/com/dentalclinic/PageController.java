package com.dentalclinic;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final DashboardService dashboard;

    public PageController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/index.html"})
    public String home(Principal principal, Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("date", today());
        model.addAttribute("totals", dashboard.totals(today()));
        return "home";
    }

    @GetMapping("/today/appointments")
    public String appointments(Model model) {
        return appointmentDetails(model, false);
    }

    @GetMapping("/today/arrivals")
    public String arrivals(Model model) {
        return appointmentDetails(model, true);
    }

    private String appointmentDetails(Model model, boolean arrivalsOnly) {
        model.addAttribute("title", arrivalsOnly ? "Today's arrivals" : "Today's appointments");
        model.addAttribute("date", today());
        model.addAttribute("rows", dashboard.appointments(today(), arrivalsOnly));
        model.addAttribute("payments", false);
        return "today-details";
    }

    @GetMapping("/today/collections")
    public String collections(Model model) {
        model.addAttribute("title", "Today's collections");
        model.addAttribute("date", today());
        model.addAttribute("rows", dashboard.payments(today()));
        model.addAttribute("payments", true);
        return "today-details";
    }
}
