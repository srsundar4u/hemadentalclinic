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
}
