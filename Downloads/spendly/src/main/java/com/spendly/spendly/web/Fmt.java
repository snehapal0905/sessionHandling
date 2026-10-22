package com.spendly.spendly.web;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Template helper, used as ${@fmt.money(x)} etc. Amounts use the signed-in user's currency. */
@Component("fmt")
@RequiredArgsConstructor
public class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final CurrentUser currentUser;

    public String symbol() {
        AppUser user = currentUser.find();
        return user == null ? "₹" : user.getCurrencySymbol();
    }

    /** ₹1,25,000.50 (Indian grouping for ₹, Western grouping otherwise). */
    public String money(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        String cents = v.abs().toPlainString();
        cents = cents.substring(cents.indexOf('.'));
        return (v.signum() < 0 ? "-" : "") + symbol() + group(v.abs().longValue()) + cents;
    }

    /** ₹1,25,001 - rounded to whole units, for tiles and summaries. */
    public String whole(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
        return (v.signum() < 0 ? "-" : "") + symbol() + group(v.abs().longValue());
    }

    /** +₹500 / -₹500 */
    public String signed(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return (v.signum() > 0 ? "+" : "") + whole(v);
    }

    /** Amount without symbol, two decimals, for form inputs. */
    public String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Whole number without symbol or grouping, for number inputs (e.g. budget limits). */
    public String integer(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /** part / whole in whole percent. */
    public int percent(BigDecimal part, BigDecimal whole) {
        return com.spendly.spendly.service.Money.percent(part, whole);
    }

    /** Clamps a percentage to 0-100 for bar widths. */
    public int bar(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public BigDecimal atLeastZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    public String percentChange(int change) {
        return (change > 0 ? "+" : "") + change + "%";
    }

    public String group(long v) {
        String s = Long.toString(v);
        if (s.length() <= 3) return s;
        String last3 = s.substring(s.length() - 3);
        String rest = s.substring(0, s.length() - 3);
        int size = "₹".equals(symbol()) ? 2 : 3;
        StringBuilder out = new StringBuilder();
        while (rest.length() > size) {
            out.insert(0, "," + rest.substring(rest.length() - size));
            rest = rest.substring(0, rest.length() - size);
        }
        return out.insert(0, rest).append(',').append(last3).toString();
    }

    public String date(LocalDate d) {
        return d == null ? "-" : d.format(DATE);
    }

    public String shortDate(LocalDate d) {
        return d == null ? "-" : d.format(SHORT);
    }

    public String day(LocalDate d) {
        return d == null ? "-" : d.format(DAY);
    }

    public String dateTime(LocalDateTime d) {
        return d == null ? "-" : d.format(DATE_TIME);
    }

    public String month(YearMonth m) {
        return m == null ? "" : m.format(MONTH);
    }

    /** Value for ?month= links, e.g. 2026-10. */
    public String param(YearMonth m) {
        return m == null ? "" : m.toString();
    }

    public String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, 1) : parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1);
    }

    /** "in 3 days", "today", "2 days late". */
    public String due(long daysAway) {
        if (daysAway == 0) return "Today";
        if (daysAway == 1) return "Tomorrow";
        if (daysAway < 0) return -daysAway + (daysAway == -1 ? " day late" : " days late");
        return "In " + daysAway + " days";
    }
}
