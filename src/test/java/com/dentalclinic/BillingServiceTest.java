package com.dentalclinic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingServiceTest {
    private final BillingRepository billing = mock(BillingRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final BillingService service = new BillingService(billing, appointments);
    private void status(String status) {
        when(appointments.find(4)).thenReturn(Optional.of(new Appointment(4, 7, "Synthetic Patient", LocalDateTime.now(), 30, "", status, null)));
    }
    private PaymentForm payment(String amount) {
        var form = new PaymentForm(); form.setAmount(new BigDecimal(amount)); form.setMethod("CASH"); form.setRequestToken("synthetic-token"); return form;
    }
    @Test
    void partialPaymentsAllowedButOverpaymentsRejected() {
        status("COMPLETED");
        when(billing.summary(4)).thenReturn(new BillingRepository.Summary(new BigDecimal("1000.00"), new BigDecimal("400.00")));
        var partial = payment("200.00"); service.pay(4, partial);
        verify(billing).addPayment(eq(4L), eq(partial), any());
        assertThrows(IllegalArgumentException.class, () -> service.pay(4, payment("600.01")));
        service.pay(4, payment("600.00"));
        verify(billing, times(2)).addPayment(eq(4L), any(), any());
        verify(billing, times(3)).lock(4);
    }
    @Test
    void repeatedSubmissionDoesNotCreateDuplicateEntry() {
        status("COMPLETED"); when(billing.paymentRecorded("synthetic-token")).thenReturn(true);
        service.pay(4, payment("200")); verify(billing, never()).addPayment(anyLong(), any(), any());
        var charge = new ChargeForm(); charge.setRequestToken("synthetic-token");
        when(billing.chargeRecorded("synthetic-token")).thenReturn(true);
        service.charge(4, charge); verify(billing, never()).addCharge(anyLong(), any(), any());
    }
    @Test
    void onlyCompletedVisitsCanBeBilled() {
        status("ARRIVED");
        assertThrows(IllegalArgumentException.class, () -> service.pay(4, payment("200")));
        assertThrows(IllegalArgumentException.class, () -> service.charge(4, new ChargeForm()));
    }
}
