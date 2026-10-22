package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    /** Shown in front of every amount, e.g. ₹ or $. */
    @Column(nullable = false, length = 5)
    private String currencySymbol = "₹";

    /** Share of income the user wants to keep each month; drives "safe to spend" and the health score. */
    private int savingsTargetPercent = 20;

    private boolean active = true;

    private LocalDateTime lastLoginAt;

    public String getFirstName() {
        return fullName == null ? "" : fullName.trim().split("\\s+")[0];
    }
}
