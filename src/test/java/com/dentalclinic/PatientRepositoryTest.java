package com.dentalclinic;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

class PatientRepositoryTest {
    private SingleConnectionDataSource dataSource;
    private PatientRepository patients;

    @BeforeEach
    void setup() {
        dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__create_clinic_tables.sql")).execute(dataSource);
        patients = new PatientRepository(new JdbcTemplate(dataSource));
    }

    @AfterEach
    void close() { dataSource.destroy(); }

    @Test
    void registerSearchAndUpdatePersistDetails() {
        var form = new PatientForm();
        form.setFullName("  Synthetic O'Brien  ");
        form.setPhone("0000000000");
        long id = patients.create(form);
        assertTrue(id > 0);
        assertEquals("Synthetic O'Brien", patients.find(id).orElseThrow().fullName());
        assertEquals(1, patients.search("o'BRIEN").size());
        assertEquals(1, patients.search("000000").size());
        assertEquals(1, patients.search(Long.toString(id)).size());
        assertTrue(patients.search("' OR 1=1 --").isEmpty());
        assertTrue(patients.search("%").isEmpty());
        assertTrue(patients.search("_").isEmpty());
        form.setFullName("Updated Synthetic Patient");
        assertTrue(patients.update(id, form));
        assertEquals("Updated Synthetic Patient", patients.find(id).orElseThrow().fullName());
        assertFalse(patients.update(9999, form));
        assertTrue(patients.find(9999).isEmpty());
    }

    @Test
    void searchFindsPatientsOutsideFirstHundred() {
        var form = new PatientForm(); form.setPhone("9876543210");
        for (int n = 0; n < 105; n++) { form.setFullName("A Synthetic " + n); patients.create(form); }
        form.setFullName("Z Unique Synthetic Patient");
        long id = patients.create(form);
        assertEquals(100, patients.search("").size());
        assertEquals(id, patients.search("Z Unique").getFirst().id());
        assertEquals(id, patients.search(Long.toString(id)).getFirst().id());
    }

    @Test
    void appointmentRepositorySavesFiltersAndReschedules() {
        var form = new PatientForm(); form.setFullName("Synthetic Patient"); form.setPhone("9876543210");
        long patientId = patients.create(form);
        var repository = new AppointmentRepository(new JdbcTemplate(dataSource));
        var booking = new AppointmentForm(); booking.setPatientId(patientId);
        var date = java.time.LocalDate.of(2026, 10, 10);
        booking.setStartsAt(date.atTime(10, 0)); booking.setDurationMinutes(30);
        long id = repository.create(booking);
        assertEquals(1, repository.onDate(date, "", false).size());
        assertEquals(1, repository.forPatient(patientId).size());
        assertTrue(repository.onDate(date, "", true).isEmpty());
        booking.setStartsAt(date.atTime(11, 0)); repository.reschedule(id, booking);
        assertEquals(date.atTime(11, 0), repository.find(id).orElseThrow().startsAt());
        repository.changeStatus(id, "ARRIVED", date.atTime(11, 0));
        assertEquals(1, repository.onDate(date, "ARRIVED", true).size());
        assertTrue(repository.onDate(date, "SCHEDULED", false).isEmpty());
        assertTrue(repository.onDate(date.plusDays(1), "", false).isEmpty());
    }
}
