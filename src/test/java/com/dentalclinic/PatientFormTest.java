package com.dentalclinic;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PatientFormTest {
    @Test
    void ageUsesBirthdayAndOptionalDetailsAreValidated() {
        var date = java.time.LocalDate.of(2026, 10, 9);
        var patient = new Patient(1, "Synthetic Patient", "9876543210", java.time.LocalDate.of(2000, 10, 10), null);
        assertEquals(25, patient.ageOn(date));
        assertEquals(26, patient.ageOn(date.plusDays(1)));
        assertNull(new Patient(1, "Synthetic Patient", "9876543210", null, null).ageOn(date));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var form = new PatientForm(); form.setFullName("Synthetic Patient"); form.setPhone("9876543210");
            assertTrue(validator.validate(form).isEmpty());
            form.setSex("FEMALE"); form.setWeightKg(new java.math.BigDecimal("62.50")); form.setHeightCm(new java.math.BigDecimal("165.25"));
            assertTrue(validator.validate(form).isEmpty());
            form.setSex("INVALID"); form.setWeightKg(java.math.BigDecimal.ZERO); form.setHeightCm(new java.math.BigDecimal("-1"));
            assertEquals(3, validator.validate(form).size());
            form.setSex(null); form.setWeightKg(new java.math.BigDecimal("62.555")); form.setHeightCm(null);
            assertFalse(validator.validate(form).isEmpty());
        }
    }

    @Test
    void phoneRequiresTenDigitsAndNonzeroFirstDigit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var form = new PatientForm();
            form.setFullName("Synthetic Patient");
            for (String phone : new String[]{"1234567890", "9876543210"}) {
                form.setPhone(phone);
                assertTrue(validator.validate(form).isEmpty(), phone);
            }
            for (String phone : new String[]{"0123456789", "123456789", "12345678901", "12345abcde", "+1234567890", "12345-6789", "", "----------"}) {
                form.setPhone(phone);
                assertFalse(validator.validate(form).isEmpty(), phone);
            }
        }
    }
}
