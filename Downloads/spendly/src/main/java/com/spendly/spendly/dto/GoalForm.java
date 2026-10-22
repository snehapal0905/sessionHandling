package com.spendly.spendly.dto;

import com.spendly.spendly.domain.SavingsGoal;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GoalForm {

    private Long id;

    @NotBlank(message = "Name your goal")
    @Size(max = 80)
    private String name;

    @NotNull(message = "How much do you need?")
    @DecimalMin(value = "1", message = "Must be more than zero")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal targetAmount;

    @NotNull(message = "Enter what you have saved (0 is fine)")
    @DecimalMin(value = "0", message = "Cannot be negative")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    private LocalDate targetDate;

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Use a colour like #b5562a")
    private String color = "#b5562a";

    @Size(max = 300)
    private String notes;

    public boolean isNew() {
        return id == null;
    }

    public static GoalForm of(SavingsGoal g) {
        GoalForm f = new GoalForm();
        f.id = g.getId();
        f.name = g.getName();
        f.targetAmount = g.getTargetAmount();
        f.savedAmount = g.getSavedAmount();
        f.targetDate = g.getTargetDate();
        f.color = g.getColor();
        f.notes = g.getNotes();
        return f;
    }
}
