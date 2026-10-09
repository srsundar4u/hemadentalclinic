package com.dentalclinic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final AppointmentService service = new AppointmentService(appointments, patients);
    private final LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(1).withSecond(0).withNano(0);

    @BeforeEach
    void setup() {
        when(patients.find(7)).thenReturn(Optional.of(new Patient(7, "Synthetic Patient", "9876543210", null, null)));
        when(appointments.active()).thenReturn(List.of(new Appointment(4, 7, "Synthetic Patient", start, 30, "", "SCHEDULED", null)));
    }

    private AppointmentForm form(LocalDateTime time) {
        var form = new AppointmentForm(); form.setPatientId(7L); form.setStartsAt(time); form.setDurationMinutes(30); return form;
    }

    @Test
    void rejectsOverlapButAllowsAdjacentSlots() {
        assertThrows(AppointmentService.SlotUnavailable.class, () -> service.book(form(start)));
        assertThrows(IllegalArgumentException.class, () -> service.book(form(start.plusMinutes(15))));
        assertThrows(IllegalArgumentException.class, () -> service.book(form(start.minusMinutes(15))));
        service.book(form(start.plusMinutes(30)));
        service.book(form(start.minusMinutes(30)));
        verify(appointments, times(2)).create(any());
        verify(appointments, times(5)).lockSchedule();
    }

    @Test
    void rescheduleExcludesItselfAndKeepsOriginalPatient() {
        Appointment existing = appointments.active().getFirst();
        when(appointments.find(4)).thenReturn(Optional.of(existing));
        var form = form(start); form.setPatientId(99L);
        service.reschedule(4, form);
        assertEquals(7L, form.getPatientId());
        verify(appointments).reschedule(4, form);
    }

    @Test
    void rescheduleRejectsAnotherAppointmentsSlot() {
        var current = new Appointment(5, 7, "Synthetic Patient", start.plusHours(2), 30, "", "SCHEDULED", null);
        when(appointments.find(5)).thenReturn(Optional.of(current));
        assertThrows(AppointmentService.SlotUnavailable.class, () -> service.reschedule(5, form(start)));
        verify(appointments, never()).reschedule(anyLong(), any());
    }

    @Test
    void rejectsPastBookingAndMissingPatient() {
        assertThrows(IllegalArgumentException.class, () -> service.book(form(start.minusDays(2))));
        var form = form(start.plusHours(1)); form.setPatientId(99L);
        assertThrows(IllegalArgumentException.class, () -> service.book(form));
        verify(appointments, never()).create(any());
    }

    @Test
    void arrivalIsRecordedOnceAndInvalidTransitionsAreRejected() {
        var current = new Appointment(4, 7, "Synthetic Patient", LocalDateTime.now(ZoneId.of("Asia/Kolkata")), 30, "", "SCHEDULED", null);
        when(appointments.find(4)).thenReturn(Optional.of(current));
        service.changeStatus(4, "ARRIVED");
        verify(appointments).changeStatus(eq(4L), eq("ARRIVED"), any(LocalDateTime.class));
        when(appointments.find(4)).thenReturn(Optional.of(new Appointment(4, 7, "Synthetic Patient", start, 30, "", "ARRIVED", start)));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(4, "CANCELLED"));
        Appointment existing = appointments.active().getFirst();
        when(appointments.find(4)).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(4, "NO_SHOW"));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(4, "ARRIVED"));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(4, "COMPLETED"));
        service.changeStatus(4, "CANCELLED");
        verify(appointments).changeStatus(4, "CANCELLED", null);
    }
}
