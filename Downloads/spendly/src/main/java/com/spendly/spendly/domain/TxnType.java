package com.spendly.spendly.domain;

public enum TxnType {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer");

    private final String label;

    TxnType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
