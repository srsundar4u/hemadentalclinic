package com.dentalclinic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BillingRepository {
    public record Summary(BigDecimal charged, BigDecimal paid) {
        public BigDecimal balance() { return charged.subtract(paid); }
    }
    private final JdbcTemplate jdbc;
    public BillingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void lock(long id) {
        jdbc.queryForObject("SELECT id FROM appointments WHERE id = ? FOR UPDATE", Long.class, id);
    }
    public Summary summary(long id) {
        return new Summary(jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM charges WHERE appointment_id = ?", BigDecimal.class, id),
            jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE appointment_id = ?", BigDecimal.class, id));
    }
    public Summary forPatient(long patientId) {
        return new Summary(jdbc.queryForObject("SELECT COALESCE(SUM(c.amount), 0) FROM charges c JOIN appointments a ON a.id = c.appointment_id WHERE a.patient_id = ?", BigDecimal.class, patientId),
            jdbc.queryForObject("SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN appointments a ON a.id = p.appointment_id WHERE a.patient_id = ?", BigDecimal.class, patientId));
    }
    public List<Map<String, Object>> charges(long id) {
        return jdbc.queryForList("SELECT description, amount, charged_at FROM charges WHERE appointment_id = ? ORDER BY charged_at, id", id);
    }
    public List<Map<String, Object>> payments(long id) {
        return jdbc.queryForList("SELECT amount, method, paid_at FROM payments WHERE appointment_id = ? ORDER BY paid_at, id", id);
    }
    public boolean chargeRecorded(String token) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM charges WHERE request_token = ?", Long.class, token) > 0;
    }
    public boolean paymentRecorded(String token) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE request_token = ?", Long.class, token) > 0;
    }
    public void addCharge(long id, ChargeForm form, LocalDateTime now) {
        jdbc.update("INSERT INTO charges(appointment_id, description, amount, charged_at, request_token) VALUES (?, ?, ?, ?, ?)",
            id, form.getDescription(), form.getAmount(), now, form.getRequestToken());
    }
    public void addPayment(long id, PaymentForm form, LocalDateTime now) {
        jdbc.update("INSERT INTO payments(appointment_id, amount, method, paid_at, request_token) VALUES (?, ?, ?, ?, ?)",
            id, form.getAmount(), form.getMethod(), now, form.getRequestToken());
    }
    public List<Map<String, Object>> list(String query) {
        String term = query.trim().toLowerCase(java.util.Locale.ROOT);
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.queryForList("""
            SELECT a.id, a.starts_at, p.full_name,
              COALESCE((SELECT SUM(c.amount) FROM charges c WHERE c.appointment_id = a.id), 0) AS charged,
              COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.appointment_id = a.id), 0) AS paid
            FROM appointments a JOIN patients p ON p.id = a.patient_id
            WHERE a.status = 'COMPLETED' AND (LOWER(p.full_name) LIKE ? ESCAPE '!' OR CAST(p.id AS VARCHAR) = ?)
            ORDER BY a.starts_at DESC, a.id DESC LIMIT 100
            """, pattern, term);
    }
}
