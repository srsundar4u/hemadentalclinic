package com.dentalclinic;

import java.time.LocalDateTime;

public record Consultation(long appointmentId, ConsultationForm notes, LocalDateTime updatedAt,
        LocalDateTime completedAt) {
    public boolean completed() { return completedAt != null; }
}
