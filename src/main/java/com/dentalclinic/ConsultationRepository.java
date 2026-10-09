package com.dentalclinic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultationRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Consultation> mapper = (rs, row) -> {
        var notes = new ConsultationForm();
        notes.setComplaint(rs.getString("complaint")); notes.setMedicalHistory(rs.getString("medical_history"));
        notes.setAllergies(rs.getString("allergies")); notes.setExamination(rs.getString("examination"));
        notes.setTreatment(rs.getString("treatment")); notes.setFollowUp(rs.getString("follow_up"));
        notes.setPrescription(rs.getString("prescription"));
        notes.setFollowUpDate(rs.getObject("follow_up_date", java.time.LocalDate.class));
        notes.setVersion(rs.getLong("version"));
        return new Consultation(rs.getLong("appointment_id"), notes,
            rs.getObject("updated_at", LocalDateTime.class), rs.getObject("completed_at", LocalDateTime.class));
    };

    public ConsultationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Consultation> find(long appointmentId) {
        return jdbc.query("SELECT * FROM consultations WHERE appointment_id = ?", mapper, appointmentId).stream().findFirst();
    }

    public List<Consultation> forPatient(long patientId) {
        return jdbc.query("""
            SELECT c.* FROM consultations c JOIN appointments a ON a.id = c.appointment_id
            WHERE a.patient_id = ? ORDER BY a.starts_at DESC, c.appointment_id DESC
            """, mapper, patientId);
    }

    public void create(long id, LocalDateTime now) {
        jdbc.update("INSERT INTO consultations(appointment_id, updated_at) VALUES (?, ?)", id, now);
    }

    public boolean save(long id, ConsultationForm notes, LocalDateTime now, boolean complete) {
        return jdbc.update("""
            UPDATE consultations SET complaint = ?, medical_history = ?, allergies = ?, examination = ?,
            treatment = ?, follow_up = ?, prescription = ?, follow_up_date = ?, updated_at = ?, completed_at = ?, version = version + 1
            WHERE appointment_id = ? AND version = ? AND completed_at IS NULL
            """, notes.getComplaint(), notes.getMedicalHistory(), notes.getAllergies(), notes.getExamination(),
            notes.getTreatment(), notes.getFollowUp(), notes.getPrescription(), notes.getFollowUpDate(), now, complete ? now : null,
            id, notes.getVersion()) == 1;
    }
}
