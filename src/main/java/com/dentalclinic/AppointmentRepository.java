package com.dentalclinic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AppointmentRepository {
    private final JdbcTemplate jdbc;
    private final String select = "SELECT a.*, p.full_name FROM appointments a JOIN patients p ON p.id = a.patient_id ";
    private final RowMapper<Appointment> mapper = (rs, n) -> new Appointment(
        rs.getLong("id"), rs.getLong("patient_id"), rs.getString("full_name"),
        rs.getObject("starts_at", LocalDateTime.class), rs.getInt("duration_minutes"),
        rs.getString("reason"), rs.getString("status"), rs.getObject("checked_in_at", LocalDateTime.class));

    public AppointmentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Hold this lock inside a transaction so concurrent bookings cannot both claim a slot.
    public void lockSchedule() { jdbc.execute("LOCK TABLE appointments IN SHARE ROW EXCLUSIVE MODE"); }

    public List<Appointment> onDate(LocalDate date, String status, boolean arrivals) {
        String sql = select + "WHERE a.starts_at >= ? AND a.starts_at < ?";
        if (arrivals) { sql += " AND a.checked_in_at IS NOT NULL"; }
        if (!status.isEmpty()) {
            return jdbc.query(sql + " AND a.status = ? ORDER BY a.starts_at, a.id", mapper,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(), status);
        }
        return jdbc.query(sql + " ORDER BY a.starts_at, a.id", mapper,
            date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<Appointment> forPatient(long id) {
        return jdbc.query(select + "WHERE a.patient_id = ? ORDER BY a.starts_at DESC, a.id DESC", mapper, id);
    }

    public List<Appointment> active() {
        return jdbc.query(select + "WHERE a.status IN ('SCHEDULED', 'ARRIVED', 'IN_CONSULTATION', 'COMPLETED')", mapper);
    }

    public Optional<Appointment> find(long id) {
        return jdbc.query(select + "WHERE a.id = ?", mapper, id).stream().findFirst();
    }

    public long create(AppointmentForm form) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO appointments(patient_id, starts_at, duration_minutes, reason) VALUES (?, ?, ?, ?)", new String[]{"id"});
            statement.setLong(1, form.getPatientId());
            statement.setObject(2, form.getStartsAt());
            statement.setInt(3, form.getDurationMinutes());
            statement.setString(4, form.getReason());
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }

    public void reschedule(long id, AppointmentForm form) {
        jdbc.update("UPDATE appointments SET starts_at = ?, duration_minutes = ?, reason = ? WHERE id = ?",
            form.getStartsAt(), form.getDurationMinutes(), form.getReason(), id);
    }

    public void changeStatus(long id, String status, LocalDateTime arrivedAt) {
        jdbc.update("UPDATE appointments SET status = ?, checked_in_at = ? WHERE id = ?", status, arrivedAt, id);
    }
}
