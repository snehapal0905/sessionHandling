package com.spendly.spendly.domain;

public enum AccountType {
    BANK("Bank account", true),
    CASH("Cash", true),
    WALLET("Digital wallet", true),
    SAVINGS("Savings", true),
    CREDIT_CARD("Credit card", false),
    INVESTMENT("Investment", false);

    private final String label;
    /** Money you can spend today: counts towards the emergency-fund cover in the health score. */
    private final boolean liquid;

    AccountType(String label, boolean liquid) {
        this.label = label;
        this.liquid = liquid;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLiquid() {
        return liquid;
    }
}
