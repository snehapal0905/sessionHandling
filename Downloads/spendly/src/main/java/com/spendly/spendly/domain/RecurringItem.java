package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A bill, subscription or income that repeats. Posting it records a transaction and moves the due date on. */
@Getter
@Setter
@Entity
@Table(name = "recurring_items")
public class RecurringItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 80)
    private String name;

    /** INCOME or EXPENSE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TxnType type = TxnType.EXPENSE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency = Frequency.MONTHLY;

    /** The next occurrence that has not been posted yet. */
    @Column(nullable = false)
    private LocalDate nextDueDate;

    private boolean active = true;

    public BigDecimal getMonthlyAmount() {
        return frequency.monthly(amount);
    }
}
