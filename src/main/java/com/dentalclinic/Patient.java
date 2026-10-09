package com.dentalclinic;

import java.time.LocalDate;
import java.time.Period;
import java.math.BigDecimal;

public record Patient(long id, String fullName, String phone, LocalDate dateOfBirth, String address,
        String sex, BigDecimal weightKg, BigDecimal heightCm) {
    public Patient(long id, String fullName, String phone, LocalDate dateOfBirth, String address) {
        this(id, fullName, phone, dateOfBirth, address, null, null, null);
    }

    public Integer ageOn(LocalDate date) {
        return dateOfBirth == null || dateOfBirth.isAfter(date) ? null : Period.between(dateOfBirth, date).getYears();
    }
}
