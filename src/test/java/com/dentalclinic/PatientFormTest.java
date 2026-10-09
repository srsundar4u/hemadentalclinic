package com.dentalclinic;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PatientFormTest {
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
