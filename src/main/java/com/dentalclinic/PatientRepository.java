package com.dentalclinic;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PatientRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Patient> mapper = (rs, row) -> new Patient(
        rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
        rs.getObject("date_of_birth", java.time.LocalDate.class), rs.getString("address"),
        rs.getString("sex"), rs.getBigDecimal("weight_kg"), rs.getBigDecimal("height_cm"));

    public PatientRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Patient> search(String query) {
        String term = query.trim().toLowerCase(java.util.Locale.ROOT);
        // Escape LIKE wildcards so the user's search is treated as literal text.
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.query("""
            SELECT * FROM patients
            WHERE LOWER(full_name) LIKE ? ESCAPE '!' OR phone LIKE ? ESCAPE '!'
                OR CAST(id AS VARCHAR) = ?
            ORDER BY full_name, id LIMIT 100
            """, mapper, pattern, pattern, term);
    }

    public Optional<Patient> find(long id) {
        return jdbc.query("SELECT * FROM patients WHERE id = ?", mapper, id).stream().findFirst();
    }

    public long create(PatientForm form) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO patients(full_name, phone, date_of_birth, address, sex, weight_kg, height_cm) VALUES (?, ?, ?, ?, ?, ?, ?)",
                new String[]{"id"});
            statement.setString(1, form.getFullName());
            statement.setString(2, form.getPhone());
            statement.setDate(3, form.getDateOfBirth() == null ? null : Date.valueOf(form.getDateOfBirth()));
            statement.setString(4, form.getAddress());
            statement.setString(5, form.getSex());
            statement.setBigDecimal(6, form.getWeightKg());
            statement.setBigDecimal(7, form.getHeightCm());
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }

    public boolean update(long id, PatientForm form) {
        return jdbc.update("UPDATE patients SET full_name = ?, phone = ?, date_of_birth = ?, address = ?, sex = ?, weight_kg = ?, height_cm = ? WHERE id = ?",
            form.getFullName(), form.getPhone(), form.getDateOfBirth(), form.getAddress(), form.getSex(), form.getWeightKg(), form.getHeightCm(), id) == 1;
    }
}
