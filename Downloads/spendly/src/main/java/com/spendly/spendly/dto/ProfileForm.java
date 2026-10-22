package com.spendly.spendly.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String fullName;

    @Email(message = "Enter a valid email")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Pick a currency symbol")
    @Size(max = 5)
    private String currencySymbol;

    @Min(value = 0, message = "0-90%")
    @Max(value = 90, message = "0-90%")
    private int savingsTargetPercent = 20;
}
