package com.dentalclinic;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class ChargeForm {
    @NotBlank(message = "Enter a charge description.") @Size(max = 500)
    private String description;
    @NotNull @DecimalMin(value = "0.01", message = "Enter a positive amount.") @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    @NotBlank @Pattern(regexp = "[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    private String requestToken;
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value == null ? null : value.trim(); }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String value) { requestToken = value; }
}
