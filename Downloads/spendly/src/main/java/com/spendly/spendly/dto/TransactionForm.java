package com.spendly.spendly.dto;

import com.spendly.spendly.domain.Transaction;
import com.spendly.spendly.domain.TxnType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionForm {

    private Long id;

    @NotNull
    private TxnType type = TxnType.EXPENSE;

    @NotNull(message = "Pick an account")
    private Long accountId;

    /** Transfers only. */
    private Long toAccountId;

    /** Income and expenses only. */
    private Long categoryId;

    @NotNull(message = "Enter an amount")
    @DecimalMin(value = "0.01", message = "Must be more than zero")
    @Digits(integer = 12, fraction = 2, message = "Too many digits")
    private BigDecimal amount;

    @NotNull(message = "Pick a date")
    private LocalDate date = LocalDate.now();

    @NotBlank(message = "Describe it, e.g. \"Dinner at Toit\"")
    @Size(max = 140)
    private String description;

    @Size(max = 500)
    private String notes;

    public boolean isNew() {
        return id == null;
    }

    @AssertTrue(message = "Pick a category")
    public boolean isCategoryChosen() {
        return type == TxnType.TRANSFER || categoryId != null;
    }

    @AssertTrue(message = "Pick a different account to move money to")
    public boolean isTransferTargetValid() {
        return type != TxnType.TRANSFER || (toAccountId != null && !toAccountId.equals(accountId));
    }

    public static TransactionForm of(Transaction t) {
        TransactionForm f = new TransactionForm();
        f.id = t.getId();
        f.type = t.getType();
        f.accountId = t.getAccount().getId();
        f.toAccountId = t.getToAccount() == null ? null : t.getToAccount().getId();
        f.categoryId = t.getCategory() == null ? null : t.getCategory().getId();
        f.amount = t.getAmount();
        f.date = t.getDate();
        f.description = t.getDescription();
        f.notes = t.getNotes();
        return f;
    }
}
