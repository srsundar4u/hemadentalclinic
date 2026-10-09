package com.dentalclinic;

import java.time.LocalDateTime;

public record Appointment(long id, long patientId, String patientName, LocalDateTime startsAt,
        int durationMinutes, String reason, String status, LocalDateTime checkedInAt) {
    public LocalDateTime endsAt() { return startsAt.plusMinutes(durationMinutes); }
    public boolean scheduled() { return "SCHEDULED".equals(status); }
}
