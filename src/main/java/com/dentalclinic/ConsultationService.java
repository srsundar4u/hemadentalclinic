package com.dentalclinic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultationService {
    private final AppointmentRepository appointments;
    private final ConsultationRepository consultations;
    public ConsultationService(AppointmentRepository appointments, ConsultationRepository consultations) {
        this.appointments = appointments; this.consultations = consultations;
    }

    @Transactional
    public void start(long id) {
        appointments.lockSchedule();
        var appointment = require(id);
        if ("IN_CONSULTATION".equals(appointment.status()) || "COMPLETED".equals(appointment.status())) { return; }
        if (!"ARRIVED".equals(appointment.status())) {
            throw new IllegalArgumentException("Mark the patient arrived before starting consultation.");
        }
        consultations.create(id, now());
        appointments.changeStatus(id, "IN_CONSULTATION", appointment.checkedInAt());
    }

    @Transactional
    public void save(long id, ConsultationForm form, boolean complete) {
        appointments.lockSchedule();
        var appointment = require(id);
        if (!"IN_CONSULTATION".equals(appointment.status())) {
            throw new IllegalArgumentException("Only an active consultation can be saved. Completed visits are read-only.");
        }
        if (complete && (form.getComplaint().isBlank() || form.getTreatment().isBlank())) {
            throw new IllegalArgumentException("Record the complaint and treatment before completing the visit.");
        }
        var now = now();
        if (form.getFollowUpDate() != null && form.getFollowUpDate().isBefore(now.toLocalDate())) {
            throw new IllegalArgumentException("Follow-up date cannot be in the past.");
        }
        if (!consultations.save(id, form, now, complete)) {
            throw new IllegalArgumentException("This visit changed in another window. Copy your notes, reload the page, and try again.");
        }
        if (complete) { appointments.changeStatus(id, "COMPLETED", appointment.checkedInAt()); }
    }

    private Appointment require(long id) {
        return appointments.find(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
}
