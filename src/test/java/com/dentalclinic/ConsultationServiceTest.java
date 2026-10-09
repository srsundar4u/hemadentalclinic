package com.dentalclinic;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultationServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final ConsultationRepository consultations = mock(ConsultationRepository.class);
    private final ConsultationService service = new ConsultationService(appointments, consultations);
    private final LocalDateTime now = LocalDateTime.now();

    private void status(String status) {
        when(appointments.find(4)).thenReturn(Optional.of(new Appointment(4, 7, "Synthetic Patient", now, 30, "", status, now)));
    }

    @Test
    void startRequiresArrivalAndRetainsCheckIn() {
        status("SCHEDULED"); assertThrows(IllegalArgumentException.class, () -> service.start(4));
        status("ARRIVED"); service.start(4);
        verify(consultations).create(eq(4L), any());
        verify(appointments).changeStatus(4, "IN_CONSULTATION", now);
        status("IN_CONSULTATION"); service.start(4);
        verify(consultations, times(1)).create(eq(4L), any());
    }

    @Test
    void draftAndCompletionUpdateStatusOnlyWhenAppropriate() {
        status("IN_CONSULTATION");
        var form = new ConsultationForm();
        when(consultations.save(eq(4L), eq(form), any(), anyBoolean())).thenReturn(true);
        service.save(4, form, false);
        verify(appointments, never()).changeStatus(anyLong(), anyString(), any());
        assertThrows(IllegalArgumentException.class, () -> service.save(4, form, true));
        form.setComplaint("Synthetic complaint"); form.setTreatment("Synthetic treatment");
        service.save(4, form, true);
        verify(appointments).changeStatus(4, "COMPLETED", now);
        status("COMPLETED"); assertThrows(IllegalArgumentException.class, () -> service.save(4, form, false));
    }

    @Test
    void staleDraftAndPastFollowUpAreRejected() {
        status("IN_CONSULTATION");
        var form = new ConsultationForm();
        assertThrows(IllegalArgumentException.class, () -> service.save(4, form, false));
        form.setFollowUpDate(java.time.LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.save(4, form, false));
    }
}
