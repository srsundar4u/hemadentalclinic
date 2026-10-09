package com.dentalclinic;

import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public class PatientForm {
    @NotBlank(message = "Enter the patient's name.")
    @Size(max = 150, message = "Name must be 150 characters or fewer.")
    private String fullName;

    @NotBlank(message = "Enter a phone number.")
    @Pattern(regexp = "[1-9][0-9]{9}", message = "Phone number must contain exactly 10 digits and must not start with zero.")
    private String phone;

    @PastOrPresent(message = "Date of birth cannot be in the future.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Address must be 500 characters or fewer.")
    private String address;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Choose a valid sex.")
    private String sex;
    @DecimalMin(value = "0.01", message = "Weight must be greater than zero.")
    @Digits(integer = 4, fraction = 2, message = "Weight must be at most 9999.99 kg with up to two decimal places.")
    private BigDecimal weightKg;
    @DecimalMin(value = "0.01", message = "Height must be greater than zero.")
    @Digits(integer = 4, fraction = 2, message = "Height must be at most 9999.99 cm with up to two decimal places.")
    private BigDecimal heightCm;

    public String getSex() { return sex; }
    public void setSex(String value) { sex = value == null || value.isBlank() ? null : value; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal value) { weightKg = value; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal value) { heightCm = value; }

    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value == null ? null : value.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value == null ? null : value.trim(); }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate value) { dateOfBirth = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value == null ? null : value.trim(); }
}
