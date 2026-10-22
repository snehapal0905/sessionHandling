package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Entity
@Table(name = "savings_goals")
public class SavingsGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    private LocalDate targetDate;

    @Column(nullable = false, length = 7)
    private String color = "#b5562a";

    @Column(length = 300)
    private String notes;

    public BigDecimal getRemaining() {
        return targetAmount.subtract(savedAmount).max(BigDecimal.ZERO);
    }

    public int getPercent() {
        if (targetAmount.signum() <= 0) return 100;
        return savedAmount.multiply(BigDecimal.valueOf(100)).divide(targetAmount, 0, RoundingMode.DOWN)
                .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).intValue();
    }

    public boolean isComplete() {
        return savedAmount.compareTo(targetAmount) >= 0;
    }

    /** Months left to the target date, counting the current month. Null when there is no date. */
    public Long monthsLeft(LocalDate today) {
        if (targetDate == null) return null;
        return Math.max(1, ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)) + 1);
    }

    /** What to put aside each month to reach the goal on time. Null when there is no date. */
    public BigDecimal monthlyNeeded(LocalDate today) {
        Long months = monthsLeft(today);
        return months == null ? null : getRemaining().divide(BigDecimal.valueOf(months), 0, RoundingMode.UP);
    }
}
