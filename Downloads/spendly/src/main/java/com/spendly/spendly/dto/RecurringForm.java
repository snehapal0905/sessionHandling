package com.spendly.spendly.dto;

import com.spendly.spendly.domain.Frequency;
import com.spendly.spendly.domain.RecurringItem;
import com.spendly.spendly.domain.TxnType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecurringForm {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 80)
    private String name;

    @NotNull
    private TxnType type = TxnType.EXPENSE;

    @NotNull(message = "Pick an account")
    private Long accountId;

    @NotNull(message = "Pick a category")
    private Long categoryId;

    @NotNull(message = "Enter an amount")
    @DecimalMin(value = "0.01", message = "Must be more than zero")
    @Digits(integer = 12, fraction = 2, message = "Too many digits")
    private BigDecimal amount;

    @NotNull
    private Frequency frequency = Frequency.MONTHLY;

    @NotNull(message = "When is it next due?")
    private LocalDate nextDueDate = LocalDate.now();

    private boolean active = true;

    public boolean isNew() {
        return id == null;
    }

    @AssertTrue(message = "Recurring items are income or expenses")
    public boolean isFlow() {
        return type != TxnType.TRANSFER;
    }

    public static RecurringForm of(RecurringItem r) {
        RecurringForm f = new RecurringForm();
        f.id = r.getId();
        f.name = r.getName();
        f.type = r.getType();
        f.accountId = r.getAccount().getId();
        f.categoryId = r.getCategory().getId();
        f.amount = r.getAmount();
        f.frequency = r.getFrequency();
        f.nextDueDate = r.getNextDueDate();
        f.active = r.isActive();
        return f;
    }
}
