package com.dentalclinic;

import java.time.LocalDate;

public record Patient(long id, String fullName, String phone, LocalDate dateOfBirth, String address) {}
