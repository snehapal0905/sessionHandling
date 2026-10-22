package com.spendly.spendly.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/** Small BigDecimal helpers shared by the services. */
public final class Money {

    private Money() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> f) {
        return items.stream().map(f).map(Money::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** part / whole as a whole percentage, 0 when whole is zero. */
    public static int percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return 0;
        return nz(part).multiply(BigDecimal.valueOf(100)).divide(whole.abs(), 0, RoundingMode.HALF_UP).intValue();
    }

    public static BigDecimal times(BigDecimal v, double factor) {
        return nz(v).multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
