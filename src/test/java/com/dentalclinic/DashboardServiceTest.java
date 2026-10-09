package com.dentalclinic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

class DashboardServiceTest {
    private JdbcTemplate jdbc;
    private DashboardService dashboard;
    private SingleConnectionDataSource dataSource;
    private final LocalDate date = LocalDate.of(2026, 10, 9);

    @BeforeEach
    void database() {
        dataSource = new SingleConnectionDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource(
            "db/migration/V1__create_clinic_tables.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        dashboard = new DashboardService(jdbc);
    }

    @org.junit.jupiter.api.AfterEach
    void closeDatabase() {
        dataSource.destroy();
    }

    @Test
    void emptyDatabaseShowsZeroTotalsAndEmptyDetails() {
        var totals = dashboard.totals(date);
        assertEquals(0, totals.appointments());
        assertEquals(0, totals.arrivals());
        assertEquals(0, totals.collections().compareTo(BigDecimal.ZERO));
        assertTrue(dashboard.appointments(date, false).isEmpty());
        assertTrue(dashboard.payments(date).isEmpty());
    }

    @Test
    void countsUseSelectedDayAndPaymentsCanBelongToOlderVisits() {
        jdbc.update("INSERT INTO patients(id, full_name, phone) VALUES (1, 'Synthetic Patient', '0000000000')");
        jdbc.update("""
            INSERT INTO appointments(id, patient_id, starts_at, duration_minutes, status, checked_in_at) VALUES
            (1, 1, '2026-10-09 00:00:00', 30, 'COMPLETED', '2026-10-09 00:00:00'),
            (2, 1, '2026-10-09 23:59:59', 30, 'CANCELLED', NULL),
            (3, 1, '2026-10-08 10:00:00', 30, 'COMPLETED', '2026-10-08 10:00:00'),
            (4, 1, '2026-10-10 00:00:00', 30, 'SCHEDULED', NULL)
            """);
        jdbc.update("""
            INSERT INTO payments(appointment_id, paid_at, amount, method) VALUES
            (3, '2026-10-09 00:00:00', 125.50, 'UPI'),
            (1, '2026-10-09 23:59:59', 200.25, 'CASH'),
            (1, '2026-10-10 00:00:00', 1000.00, 'CARD'),
            (3, '2026-10-08 10:00:00', 500.00, 'CASH')
            """);
        var totals = dashboard.totals(date);
        assertEquals(2, totals.appointments());
        assertEquals(1, totals.arrivals());
        assertEquals(0, totals.collections().compareTo(new BigDecimal("325.75")));
        assertEquals(2, dashboard.appointments(date, false).size());
        assertEquals(1, dashboard.appointments(date, true).size());
        assertEquals(2, dashboard.payments(date).size());
    }
}
