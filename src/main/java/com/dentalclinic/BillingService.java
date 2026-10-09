package com.dentalclinic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private final BillingRepository billing;
    private final AppointmentRepository appointments;
    public BillingService(BillingRepository billing, AppointmentRepository appointments) { this.billing = billing; this.appointments = appointments; }

    @Transactional
    public void charge(long id, ChargeForm form) {
        billing.lock(id); requireCompleted(id);
        if (billing.chargeRecorded(form.getRequestToken())) { return; }
        billing.addCharge(id, form, now());
    }

    @Transactional
    public void pay(long id, PaymentForm form) {
        billing.lock(id); requireCompleted(id);
        if (billing.paymentRecorded(form.getRequestToken())) { return; }
        if (form.getAmount().compareTo(billing.summary(id).balance()) > 0) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding balance. Refresh the bill to check the latest amount.");
        }
        billing.addPayment(id, form, now());
    }

    private void requireCompleted(long id) {
        if (!appointments.find(id).map(a -> "COMPLETED".equals(a.status())).orElse(false)) {
            throw new IllegalArgumentException("Charges and payments can be recorded after the visit is completed.");
        }
    }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
}
