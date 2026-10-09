package com.dentalclinic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Totals(long appointments, long arrivals, BigDecimal collections) {}

    public Totals totals(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Long appointments = jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointments WHERE starts_at >= ? AND starts_at < ?",
            Long.class, start, end);
        Long arrivals = jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointments WHERE starts_at >= ? AND starts_at < ? AND checked_in_at IS NOT NULL",
            Long.class, start, end);
        BigDecimal collections = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE paid_at >= ? AND paid_at < ?",
            BigDecimal.class, start, end);
        return new Totals(appointments, arrivals, collections);
    }

    public List<Map<String, Object>> appointments(LocalDate date, boolean arrivalsOnly) {
        String sql = """
            SELECT p.full_name, a.starts_at, a.status
            FROM appointments a JOIN patients p ON p.id = a.patient_id
            WHERE a.starts_at >= ? AND a.starts_at < ?
            """;
        if (arrivalsOnly) {
            sql += " AND a.checked_in_at IS NOT NULL";
        }
        return jdbc.queryForList(sql + " ORDER BY a.starts_at, a.id",
            date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<Map<String, Object>> payments(LocalDate date) {
        return jdbc.queryForList("""
            SELECT p.full_name, pay.paid_at, pay.amount, pay.method
            FROM payments pay JOIN appointments a ON a.id = pay.appointment_id
            JOIN patients p ON p.id = a.patient_id
            WHERE pay.paid_at >= ? AND pay.paid_at < ?
            ORDER BY pay.paid_at, pay.id
            """, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
