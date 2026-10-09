package com.dentalclinic;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public class ConsultationForm {
    @Size(max = 4000) private String complaint = "";
    @Size(max = 4000) private String medicalHistory = "";
    @Size(max = 4000) private String allergies = "";
    @Size(max = 4000) private String examination = "";
    @Size(max = 4000) private String treatment = "";
    @Size(max = 4000) private String followUp = "";
    @Size(max = 8000, message = "Prescription must be 8000 characters or fewer.") private String prescription = "";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate followUpDate;
    @Min(0) private long version;
    public String getComplaint() { return complaint; }
    public void setComplaint(String value) { complaint = clean(value); }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String value) { medicalHistory = clean(value); }
    public String getAllergies() { return allergies; }
    public void setAllergies(String value) { allergies = clean(value); }
    public String getExamination() { return examination; }
    public void setExamination(String value) { examination = clean(value); }
    public String getTreatment() { return treatment; }
    public void setTreatment(String value) { treatment = clean(value); }
    public String getFollowUp() { return followUp; }
    public String getPrescription() { return prescription; }
    public void setPrescription(String value) { prescription = clean(value); }
    public void setFollowUp(String value) { followUp = clean(value); }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate value) { followUpDate = value; }
    public long getVersion() { return version; }
    public void setVersion(long value) { version = value; }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
