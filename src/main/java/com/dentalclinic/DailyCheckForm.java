package com.dentalclinic;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public class DailyCheckForm {
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    @NotNull @Pattern(regexp = "PENDING|DONE|NOT_REQUIRED", message = "Choose a valid sterilization status.")
    private String sterilizationStatus = "PENDING";
    @NotNull @Pattern(regexp = "PENDING|DONE|NOT_REQUIRED", message = "Choose a valid lab status.")
    private String labStatus = "PENDING";
    @Size(max = 2000, message = "Remarks must be 2000 characters or fewer.")
    private String remarks = "";
    @Min(-1) private long version = -1;
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public String getSterilizationStatus() { return sterilizationStatus; }
    public void setSterilizationStatus(String value) { sterilizationStatus = value; }
    public String getLabStatus() { return labStatus; }
    public void setLabStatus(String value) { labStatus = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value == null ? "" : value.trim(); }
    public long getVersion() { return version; }
    public void setVersion(long value) { version = value; }
}
