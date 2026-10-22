package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line in the user's money ledger. Amounts are always positive; the type says which way the money moved.
 * A transfer moves money from {@link #account} to {@link #toAccount} and has no category.
 */
@Getter
@Setter
@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_txn_user_date", columnList = "user_id, txn_date"))
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TxnType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 140)
    private String description;

    @Column(length = 500)
    private String notes;

    /** Set when the line was posted from a recurring bill or income. */
    private Long recurringItemId;
}
