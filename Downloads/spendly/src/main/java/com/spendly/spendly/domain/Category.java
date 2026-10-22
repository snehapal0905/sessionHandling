package com.spendly.spendly.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryKind kind = CategoryKind.EXPENSE;

    /** Chart colour, e.g. #b5562a. */
    @Column(nullable = false, length = 7)
    private String color = "#b5562a";

    /** Needs (rent, groceries) vs wants (dining out, shopping) - used for the needs / wants split. */
    private boolean essential;

    private boolean active = true;
}
