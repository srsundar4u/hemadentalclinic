package com.dentalclinic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/consultations")
public class ConsultationController {
    private final ConsultationRepository consultations;
    private final AppointmentRepository appointments;
    private final ConsultationService service;
    public ConsultationController(ConsultationRepository consultations, AppointmentRepository appointments, ConsultationService service) {
        this.consultations = consultations; this.appointments = appointments; this.service = service;
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable long id, RedirectAttributes redirect) {
        try { service.start(id); return "redirect:/consultations/" + id; }
        catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage()); return "redirect:/appointments/" + id;
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable long id, Model model) {
        var consultation = consultations.find(id).orElseThrow(MissingVisit::new);
        model.addAttribute("consultationForm", consultation.notes());
        context(id, consultation, model); return "consultations/visit";
    }

    @PostMapping("/{id}")
    public String save(@PathVariable long id, @Valid @ModelAttribute ConsultationForm consultationForm,
            BindingResult errors, @RequestParam String action, Model model, RedirectAttributes redirect) {
        var consultation = consultations.find(id).orElseThrow(MissingVisit::new);
        if (!action.equals("draft") && !action.equals("complete") && !action.equals("print")) {
            errors.reject("action", "Choose Save draft, Complete visit, or Save and print.");
        }
        if (action.equals("print") && consultationForm.getPrescription().isBlank()) {
            errors.rejectValue("prescription", "empty", "Enter a prescription before printing.");
        }
        if (!errors.hasErrors()) {
            try {
                service.save(id, consultationForm, action.equals("complete"));
                if (action.equals("print")) { return "redirect:/consultations/" + id + "/prescription"; }
                redirect.addFlashAttribute("message", action.equals("complete") ? "Visit completed." : "Draft saved.");
                return "redirect:/consultations/" + id;
            } catch (IllegalArgumentException ex) { errors.reject("save", ex.getMessage()); }
        }
        context(id, consultation, model); return "consultations/visit";
    }

    private void context(long id, Consultation visit, Model model) {
        model.addAttribute("visit", visit);
        model.addAttribute("appointment", appointments.find(id).orElseThrow(MissingVisit::new));
    }

    @GetMapping("/{id}/prescription")
    public String prescription(@PathVariable long id, Model model) {
        var visit = consultations.find(id).orElseThrow(MissingVisit::new);
        context(id, visit, model);
        return "consultations/prescription";
    }
    private static class MissingVisit extends RuntimeException {}
    @ExceptionHandler(MissingVisit.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing() { return "consultations/not-found"; }
}
