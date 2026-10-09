package com.dentalclinic;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public class PaymentForm {
    @NotNull @DecimalMin(value = "0.01", message = "Enter a positive amount.") @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    @NotBlank @Pattern(regexp = "CASH|UPI|CARD", message = "Choose Cash, UPI or Card.")
    private String method;
    @NotBlank @Pattern(regexp = "[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    private String requestToken;
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getMethod() { return method; }
    public void setMethod(String value) { method = value; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String value) { requestToken = value; }
}
