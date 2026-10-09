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
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__create_consultations.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__add_prescription.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V4__add_billing.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__create_daily_checks.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V6__add_patient_details.sql")).execute(dataSource);
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
    void consultationDraftPersistsAndRejectsStaleOrCompletedUpdates() {
        var patient = new PatientForm(); patient.setFullName("Synthetic Patient"); patient.setPhone("9876543210");
        long patientId = patients.create(patient);
        var jdbc = new JdbcTemplate(dataSource);
        var appointments = new AppointmentRepository(jdbc);
        var booking = new AppointmentForm(); booking.setPatientId(patientId);
        var now = java.time.LocalDateTime.now().withNano(0);
        booking.setStartsAt(now); booking.setDurationMinutes(30);
        long id = appointments.create(booking);
        var visits = new ConsultationRepository(jdbc); visits.create(id, now);
        var form = new ConsultationForm(); form.setComplaint("Synthetic complaint");
        form.setPrescription("Synthetic prescription line 1\nPatient instructions line 2");
        assertTrue(visits.save(id, form, now, false));
        assertFalse(visits.save(id, form, now, false));
        var saved = visits.find(id).orElseThrow();
        assertEquals("Synthetic complaint", saved.notes().getComplaint());
        assertEquals("Synthetic prescription line 1\nPatient instructions line 2", saved.notes().getPrescription());
        assertEquals(1, saved.notes().getVersion());
        assertEquals(1, visits.forPatient(patientId).size());
        saved.notes().setTreatment("Synthetic treatment");
        assertTrue(visits.save(id, saved.notes(), now, true));
        var completed = visits.find(id).orElseThrow();
        assertTrue(completed.completed());
        assertFalse(visits.save(id, completed.notes(), now, false));
    }

    @Test
    void billingPersistsPartialPaymentsWithoutMultiplyingTotals() {
        var patient = new PatientForm(); patient.setFullName("Synthetic Patient"); patient.setPhone("9876543210");
        long patientId = patients.create(patient);
        var jdbc = new JdbcTemplate(dataSource);
        var appointments = new AppointmentRepository(jdbc);
        var booking = new AppointmentForm(); booking.setPatientId(patientId);
        var now = java.time.LocalDateTime.now().withNano(0); booking.setStartsAt(now); booking.setDurationMinutes(30);
        long id = appointments.create(booking); appointments.changeStatus(id, "COMPLETED", now);
        var repository = new BillingRepository(jdbc);
        var charge = new ChargeForm(); charge.setDescription("Synthetic charge"); charge.setAmount(new java.math.BigDecimal("500.00"));
        charge.setRequestToken(java.util.UUID.randomUUID().toString()); repository.addCharge(id, charge, now);
        charge.setRequestToken(java.util.UUID.randomUUID().toString()); repository.addCharge(id, charge, now);
        var payment = new PaymentForm(); payment.setAmount(new java.math.BigDecimal("200.00")); payment.setMethod("UPI");
        payment.setRequestToken(java.util.UUID.randomUUID().toString()); repository.addPayment(id, payment, now);
        payment.setRequestToken(java.util.UUID.randomUUID().toString()); repository.addPayment(id, payment, now);
        assertEquals(0, repository.summary(id).balance().compareTo(new java.math.BigDecimal("600.00")));
        assertEquals(0, repository.forPatient(patientId).balance().compareTo(new java.math.BigDecimal("600.00")));
        assertEquals(2, repository.payments(id).size()); assertEquals(2, repository.charges(id).size());
        assertTrue(repository.paymentRecorded(payment.getRequestToken()));
        assertEquals(1, repository.list(Long.toString(patientId)).size());
        assertEquals(0, new DashboardService(jdbc).totals(now.toLocalDate()).collections().compareTo(new java.math.BigDecimal("400.00")));
    }

    @Test
    void dailyChecksPersistByDateAndRejectStaleSaves() {
        var repository = new DailyCheckRepository(new JdbcTemplate(dataSource));
        var date = java.time.LocalDate.of(2026, 10, 9);
        var now = date.atTime(16, 0);
        var form = new DailyCheckForm(); form.setDate(date); form.setSterilizationStatus("DONE");
        form.setLabStatus("PENDING"); form.setRemarks("Synthetic lab work pending");
        assertTrue(repository.save(form, "receptionist", now));
        assertFalse(repository.save(form, "dentist", now));
        var saved = repository.find(date).orElseThrow();
        assertEquals("DONE", saved.form().getSterilizationStatus());
        assertEquals("PENDING", saved.form().getLabStatus());
        assertEquals("receptionist", saved.updatedBy());
        saved.form().setLabStatus("DONE");
        assertTrue(repository.save(saved.form(), "dentist", now.plusHours(1)));
        assertFalse(repository.save(saved.form(), "receptionist", now.plusHours(2)));
        assertEquals("dentist", repository.find(date).orElseThrow().updatedBy());
        assertTrue(repository.find(date.plusDays(1)).isEmpty());
        var next = new DailyCheckForm(); next.setDate(date.plusDays(1));
        assertTrue(repository.save(next, "dentist", now.plusDays(1)));
        assertEquals("PENDING", repository.find(date.plusDays(1)).orElseThrow().form().getLabStatus());
        assertEquals("DONE", repository.find(date).orElseThrow().form().getLabStatus());
    }

    @Test
    void patientSexAndMeasurementsCanBeSavedEditedAndCleared() {
        var form = new PatientForm(); form.setFullName("Synthetic Patient"); form.setPhone("9876543210");
        form.setSex("FEMALE"); form.setWeightKg(new java.math.BigDecimal("62.50")); form.setHeightCm(new java.math.BigDecimal("165.25"));
        long id = patients.create(form);
        var patient = patients.find(id).orElseThrow();
        assertEquals("FEMALE", patient.sex());
        assertEquals(0, patient.weightKg().compareTo(new java.math.BigDecimal("62.50")));
        assertEquals(0, patient.heightCm().compareTo(new java.math.BigDecimal("165.25")));
        form.setWeightKg(new java.math.BigDecimal("63.00")); assertTrue(patients.update(id, form));
        assertEquals(0, patients.find(id).orElseThrow().weightKg().compareTo(new java.math.BigDecimal("63.00")));
        form.setSex(""); form.setWeightKg(null); form.setHeightCm(null); assertTrue(patients.update(id, form));
        patient = patients.find(id).orElseThrow();
        assertNull(patient.sex()); assertNull(patient.weightKg()); assertNull(patient.heightCm());
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
