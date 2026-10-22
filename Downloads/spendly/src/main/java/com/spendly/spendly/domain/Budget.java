package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Spending limit for one expense category in one month. */
@Getter
@Setter
@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(name = "uk_budget_period", columnNames = {"user_id", "category_id", "period_start"}))
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** First day of the budgeted month. */
    @Column(name = "period_start", nullable = false)
    private LocalDate period;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
}
