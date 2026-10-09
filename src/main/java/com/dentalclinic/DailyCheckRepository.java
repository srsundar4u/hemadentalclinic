package com.dentalclinic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DailyCheckRepository {
    public record Entry(DailyCheckForm form, String updatedBy, LocalDateTime updatedAt) {}
    private final JdbcTemplate jdbc;
    public DailyCheckRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Entry> find(LocalDate date) {
        return jdbc.query("SELECT * FROM daily_checks WHERE check_date = ?", (rs, row) -> {
            var form = new DailyCheckForm(); form.setDate(rs.getObject("check_date", LocalDate.class));
            form.setSterilizationStatus(rs.getString("sterilization_status")); form.setLabStatus(rs.getString("lab_status"));
            form.setRemarks(rs.getString("remarks")); form.setVersion(rs.getLong("version"));
            return new Entry(form, rs.getString("updated_by"), rs.getObject("updated_at", LocalDateTime.class));
        }, date).stream().findFirst();
    }

    public boolean save(DailyCheckForm form, String username, LocalDateTime now) {
        if (form.getVersion() == -1) {
            try {
                jdbc.update("""
                    INSERT INTO daily_checks(check_date, sterilization_status, lab_status, remarks, updated_by, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, form.getDate(), form.getSterilizationStatus(), form.getLabStatus(), form.getRemarks(), username, now);
                return true;
            } catch (DuplicateKeyException ex) { return false; }
        }
        return jdbc.update("""
            UPDATE daily_checks SET sterilization_status = ?, lab_status = ?, remarks = ?, updated_by = ?,
                updated_at = ?, version = version + 1
            WHERE check_date = ? AND version = ?
            """, form.getSterilizationStatus(), form.getLabStatus(), form.getRemarks(), username, now,
            form.getDate(), form.getVersion()) == 1;
    }
}
