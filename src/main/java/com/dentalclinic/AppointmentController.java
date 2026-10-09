package com.dentalclinic;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentRepository appointments;
    private final AppointmentService service;
    private final PatientRepository patients;
    private final List<String> statuses = List.of("SCHEDULED", "ARRIVED", "IN_CONSULTATION", "COMPLETED", "CANCELLED", "NO_SHOW");

    public AppointmentController(AppointmentRepository appointments, AppointmentService service, PatientRepository patients) {
        this.appointments = appointments; this.service = service; this.patients = patients;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "false") boolean arrivals,
            Model model) {
        if (date == null) { date = LocalDate.now(ZoneId.of("Asia/Kolkata")); }
        if (!status.isEmpty() && !statuses.contains(status)) { status = ""; }
        model.addAttribute("date", date); model.addAttribute("status", status);
        model.addAttribute("arrivals", arrivals); model.addAttribute("statuses", statuses);
        model.addAttribute("appointments", appointments.onDate(date, status, arrivals));
        return "appointments/list";
    }

    @GetMapping("/new")
    public String booking(@RequestParam(required = false) Long patientId, Model model) {
        var form = new AppointmentForm(); form.setPatientId(patientId);
        model.addAttribute("appointmentForm", form);
        formContext(model, false, null, form);
        return "appointments/form";
    }

    @PostMapping
    public String book(@Valid @ModelAttribute AppointmentForm appointmentForm, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                long id = service.book(appointmentForm);
                redirect.addFlashAttribute("message", "Appointment booked.");
                return "redirect:/appointments/" + id;
            } catch (AppointmentService.SlotUnavailable ex) { errors.rejectValue("startsAt", "booking", ex.getMessage()); }
            catch (IllegalArgumentException ex) { errors.reject("booking", ex.getMessage()); }
        }
        formContext(model, false, null, appointmentForm);
        return "appointments/form";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model, org.springframework.security.core.Authentication authentication) {
        model.addAttribute("dentist", authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DENTIST")));
        model.addAttribute("appointment", require(id)); return "appointments/details";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable long id, Model model) {
        var current = require(id);
        if (!current.scheduled()) { return "redirect:/appointments/" + id; }
        var form = new AppointmentForm();
        form.setPatientId(current.patientId()); form.setStartsAt(current.startsAt());
        form.setDurationMinutes(current.durationMinutes()); form.setReason(current.reason());
        model.addAttribute("appointmentForm", form);
        formContext(model, true, id, form); return "appointments/form";
    }

    @PostMapping("/{id}")
    public String reschedule(@PathVariable long id, @Valid @ModelAttribute AppointmentForm appointmentForm,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        var current = require(id);
        appointmentForm.setPatientId(current.patientId());
        if (!errors.hasErrors()) {
            try {
                service.reschedule(id, appointmentForm);
                redirect.addFlashAttribute("message", "Appointment rescheduled.");
                return "redirect:/appointments/" + id;
            } catch (AppointmentService.SlotUnavailable ex) { errors.rejectValue("startsAt", "booking", ex.getMessage()); }
            catch (IllegalArgumentException ex) { errors.reject("booking", ex.getMessage()); }
        }
        formContext(model, true, id, appointmentForm); return "appointments/form";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable long id, @RequestParam String status, RedirectAttributes redirect) {
        require(id);
        try { service.changeStatus(id, status); redirect.addFlashAttribute("message", "Appointment status updated."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/appointments/" + id;
    }

    private void formContext(Model model, boolean editing, Long id, AppointmentForm form) {
        model.addAttribute("editing", editing); model.addAttribute("appointmentId", id);
        String label = "";
        if (form.getPatientId() != null) {
            label = patients.find(form.getPatientId())
                .map(p -> p.fullName() + " — ID " + p.id() + " — " + p.phone()).orElse("");
        }
        model.addAttribute("patientLabel", label);
    }

    private Appointment require(long id) { return appointments.find(id).orElseThrow(MissingAppointment::new); }
    private static class MissingAppointment extends RuntimeException {}
    @ExceptionHandler(MissingAppointment.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() { return "appointments/not-found"; }
}
