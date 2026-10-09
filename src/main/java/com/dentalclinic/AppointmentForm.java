package com.dentalclinic;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public class AppointmentForm {
    @NotNull(message = "Select an existing patient.") @Positive
    private Long patientId;
    @NotNull(message = "Choose a date and time.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startsAt;
    @NotNull @Min(value = 5, message = "Duration must be at least 5 minutes.")
    @Max(value = 240, message = "Duration must be 240 minutes or less.")
    private Integer durationMinutes = 30;
    @Size(max = 500, message = "Reason must be 500 characters or fewer.")
    private String reason;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long value) { patientId = value; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime value) { startsAt = value; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer value) { durationMinutes = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value == null ? null : value.trim(); }
}
