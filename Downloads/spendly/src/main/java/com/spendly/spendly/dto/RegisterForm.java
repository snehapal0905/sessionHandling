package com.spendly.spendly.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Tell us your name")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Choose a username")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "3-50 letters, numbers, dots, dashes or underscores")
    private String username;

    @Email(message = "Enter a valid email")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Pick a currency symbol")
    @Size(max = 5)
    private String currencySymbol = "₹";

    @NotBlank(message = "Choose a password")
    @Size(min = 6, max = 100, message = "At least 6 characters")
    private String password;

    @NotBlank(message = "Repeat the password")
    private String confirmPassword;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        return password == null || password.equals(confirmPassword);
    }
}
