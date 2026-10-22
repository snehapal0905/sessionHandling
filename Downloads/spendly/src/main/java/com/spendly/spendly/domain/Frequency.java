package com.spendly.spendly.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public enum Frequency {
    WEEKLY("Weekly", 52, 12),
    MONTHLY("Monthly", 1, 1),
    QUARTERLY("Quarterly", 1, 3),
    YEARLY("Yearly", 1, 12);

    private final String label;
    private final int timesPer;
    private final int months;

    Frequency(String label, int timesPer, int months) {
        this.label = label;
        this.timesPer = timesPer;
        this.months = months;
    }

    public String getLabel() {
        return label;
    }

    public LocalDate next(LocalDate from) {
        return switch (this) {
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case YEARLY -> from.plusYears(1);
        };
    }

    /** What an amount at this frequency costs per month on average. */
    public BigDecimal monthly(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(timesPer)).divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
}
