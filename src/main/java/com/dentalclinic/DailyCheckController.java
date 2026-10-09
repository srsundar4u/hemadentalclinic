package com.dentalclinic;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/daily-checks")
public class DailyCheckController {
    private final DailyCheckRepository checks;
    public DailyCheckController(DailyCheckRepository checks) { this.checks = checks; }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Kolkata")); }

    @GetMapping
    public String view(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        if (date == null) { date = today(); }
        var entry = checks.find(date);
        var form = entry.map(DailyCheckRepository.Entry::form).orElseGet(DailyCheckForm::new);
        form.setDate(date);
        model.addAttribute("dailyCheckForm", form);
        model.addAttribute("entry", entry.orElse(null));
        model.addAttribute("today", today());
        return "daily-checks";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute DailyCheckForm dailyCheckForm, BindingResult errors,
            Principal principal, Model model, RedirectAttributes redirect) {
        if (dailyCheckForm.getDate() != null && dailyCheckForm.getDate().isAfter(today())) {
            errors.rejectValue("date", "future", "Daily checks cannot be saved for a future date.");
        }
        if (!errors.hasErrors()) {
            if (checks.save(dailyCheckForm, principal.getName(), LocalDateTime.now(ZoneId.of("Asia/Kolkata")))) {
                redirect.addFlashAttribute("message", "Daily checks saved.");
                return "redirect:/daily-checks?date=" + dailyCheckForm.getDate();
            }
            errors.reject("stale", "Another staff member updated this date. Copy your remarks, reload the page and try again.");
        }
        model.addAttribute("entry", dailyCheckForm.getDate() == null ? null : checks.find(dailyCheckForm.getDate()).orElse(null));
        model.addAttribute("today", today());
        return "daily-checks";
    }
}
