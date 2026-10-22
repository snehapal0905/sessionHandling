package com.spendly.spendly.dto;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountForm {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 80)
    private String name;

    @NotNull(message = "Pick a type")
    private AccountType type = AccountType.BANK;

    @Size(max = 80)
    private String institution;

    @NotNull(message = "Enter the opening balance (0 is fine)")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    private boolean active = true;

    public boolean isNew() {
        return id == null;
    }

    public static AccountForm of(Account a) {
        AccountForm f = new AccountForm();
        f.id = a.getId();
        f.name = a.getName();
        f.type = a.getType();
        f.institution = a.getInstitution();
        f.openingBalance = a.getOpeningBalance();
        f.active = a.isActive();
        return f;
    }
}
