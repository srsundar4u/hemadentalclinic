package com.dentalclinic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    public static class SlotUnavailable extends IllegalArgumentException {
        public SlotUnavailable(String message) { super(message); }
    }
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    public AppointmentService(AppointmentRepository appointments, PatientRepository patients) {
        this.appointments = appointments;
        this.patients = patients;
    }

    @Transactional
    public long book(AppointmentForm form) {
        appointments.lockSchedule();
        validateSlot(form, -1);
        return appointments.create(form);
    }

    @Transactional
    public void reschedule(long id, AppointmentForm form) {
        appointments.lockSchedule();
        Appointment current = require(id);
        if (!current.scheduled()) { throw new IllegalArgumentException("Only scheduled appointments can be rescheduled."); }
        form.setPatientId(current.patientId());
        validateSlot(form, id);
        appointments.reschedule(id, form);
    }

    private void validateSlot(AppointmentForm form, long excludeId) {
        if (patients.find(form.getPatientId()).isEmpty()) { throw new IllegalArgumentException("Select an existing patient."); }
        if (form.getStartsAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException("Choose a future time. For a walk-in, choose the next available minute.");
        }
        LocalDateTime end = form.getStartsAt().plusMinutes(form.getDurationMinutes());
        for (Appointment existing : appointments.active()) {
            if (existing.id() != excludeId && form.getStartsAt().isBefore(existing.endsAt()) && end.isAfter(existing.startsAt())) {
                throw new SlotUnavailable("An appointment already exists during this date and time. Choose a time outside its booked duration.");
            }
        }
    }

    @Transactional
    public void changeStatus(long id, String nextStatus) {
        appointments.lockSchedule();
        Appointment current = require(id);
        if (!current.scheduled()) { throw new IllegalArgumentException("This appointment is no longer scheduled. Refresh to see its current status."); }
        var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        switch (nextStatus) {
            case "ARRIVED" -> {
                if (!current.startsAt().toLocalDate().equals(now.toLocalDate())) {
                    throw new IllegalArgumentException("Arrival can be recorded only on the appointment date.");
                }
                appointments.changeStatus(id, nextStatus, now);
            }
            case "CANCELLED" -> appointments.changeStatus(id, nextStatus, null);
            case "NO_SHOW" -> {
                if (now.isBefore(current.startsAt())) { throw new IllegalArgumentException("A future appointment cannot be marked no-show."); }
                appointments.changeStatus(id, nextStatus, null);
            }
            default -> throw new IllegalArgumentException("Choose arrival, cancellation or no-show.");
        }
    }

    private Appointment require(long id) {
        return appointments.find(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }
}
