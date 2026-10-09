package com.dentalclinic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/billing")
public class BillingController {
    private final BillingRepository billing;
    private final AppointmentRepository appointments;
    private final BillingService service;
    public BillingController(BillingRepository billing, AppointmentRepository appointments, BillingService service) {
        this.billing = billing; this.appointments = appointments; this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("query", q); model.addAttribute("bills", billing.list(q)); return "billing/list";
    }
    @GetMapping("/{id}")
    public String view(@PathVariable long id, Model model) { context(id, model); return "billing/details"; }

    @PostMapping("/{id}/charges")
    public String charge(@PathVariable long id, @Valid @ModelAttribute ChargeForm chargeForm,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { service.charge(id, chargeForm); redirect.addFlashAttribute("message", "Charge recorded."); return "redirect:/billing/" + id; }
            catch (IllegalArgumentException ex) { errors.reject("charge", ex.getMessage()); }
        }
        context(id, model); return "billing/details";
    }
    @PostMapping("/{id}/payments")
    public String payment(@PathVariable long id, @Valid @ModelAttribute PaymentForm paymentForm,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { service.pay(id, paymentForm); redirect.addFlashAttribute("message", "Payment recorded."); return "redirect:/billing/" + id; }
            catch (IllegalArgumentException ex) { errors.rejectValue("amount", "payment", ex.getMessage()); }
        }
        context(id, model); return "billing/details";
    }
    private void context(long id, Model model) {
        model.addAttribute("appointment", appointments.find(id).orElseThrow(MissingBill::new));
        model.addAttribute("summary", billing.summary(id)); model.addAttribute("charges", billing.charges(id));
        model.addAttribute("payments", billing.payments(id));
        if (!model.containsAttribute("chargeForm")) {
            var form = new ChargeForm(); form.setRequestToken(java.util.UUID.randomUUID().toString()); model.addAttribute("chargeForm", form);
        }
        if (!model.containsAttribute("paymentForm")) {
            var form = new PaymentForm(); form.setRequestToken(java.util.UUID.randomUUID().toString()); model.addAttribute("paymentForm", form);
        }
    }
    private static class MissingBill extends RuntimeException {}
    @ExceptionHandler(MissingBill.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing() { return "appointments/not-found"; }
}
