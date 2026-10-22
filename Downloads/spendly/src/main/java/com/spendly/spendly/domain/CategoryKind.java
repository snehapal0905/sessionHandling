package com.spendly.spendly.domain;

public enum CategoryKind {
    INCOME("Income"),
    EXPENSE("Expense");

    private final String label;

    CategoryKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
