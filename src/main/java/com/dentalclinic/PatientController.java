package com.dentalclinic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patients")
public class PatientController {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;

    public PatientController(PatientRepository patients, AppointmentRepository appointments) {
        this.patients = patients; this.appointments = appointments;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("query", q);
        model.addAttribute("patients", patients.search(q));
        return "patients/list";
    }

    public record PatientOption(long id, String fullName, String phone) {}

    @GetMapping("/options")
    @ResponseBody
    public java.util.List<PatientOption> options(@RequestParam(defaultValue = "") String q) {
        return patients.search(q).stream().limit(20)
            .map(patient -> new PatientOption(patient.id(), patient.fullName(), patient.phone())).toList();
    }

    @GetMapping("/new")
    public String registration(Model model) {
        model.addAttribute("patientForm", new PatientForm());
        model.addAttribute("editing", false);
        return "patients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute PatientForm patientForm, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("editing", false);
            return "patients/form";
        }
        long id = patients.create(patientForm);
        redirect.addFlashAttribute("message", "Patient registered successfully.");
        return "redirect:/patients/" + id;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable long id, Model model) {
        var patient = patients.find(id);
        if (patient.isEmpty()) { throw new PatientNotFound(); }
        model.addAttribute("patient", patient.get());
        model.addAttribute("appointments", appointments.forPatient(id));
        return "patients/profile";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable long id, Model model) {
        var patient = patients.find(id);
        if (patient.isEmpty()) { throw new PatientNotFound(); }
        var form = new PatientForm();
        form.setFullName(patient.get().fullName());
        form.setPhone(patient.get().phone());
        form.setDateOfBirth(patient.get().dateOfBirth());
        form.setAddress(patient.get().address());
        model.addAttribute("patientForm", form);
        model.addAttribute("editing", true);
        model.addAttribute("patientId", id);
        return "patients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute PatientForm patientForm,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (patients.find(id).isEmpty()) { throw new PatientNotFound(); }
        if (errors.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("patientId", id);
            return "patients/form";
        }
        if (!patients.update(id, patientForm)) { throw new PatientNotFound(); }
        redirect.addFlashAttribute("message", "Patient details updated.");
        return "redirect:/patients/" + id;
    }

    private static class PatientNotFound extends RuntimeException {}

    @ExceptionHandler(PatientNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() { return "patients/not-found"; }
}
