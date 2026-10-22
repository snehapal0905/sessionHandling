package com.spendly.spendly.service;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.Transaction;
import com.spendly.spendly.domain.TxnType;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.Views.CategoryTotal;
import com.spendly.spendly.service.Views.MonthSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/** Income vs expense numbers. Transfers between your own accounts are never counted as income or spending. */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> flows(AppUser user, LocalDate from, LocalDate to) {
        return transactionRepository.findFlows(user, from, to, TxnType.TRANSFER);
    }

    public List<Transaction> flows(AppUser user, YearMonth month) {
        return flows(user, month.atDay(1), month.atEndOfMonth());
    }

    /** {@code count} months ending with {@code last}, oldest first. */
    public List<MonthSummary> months(AppUser user, YearMonth last, int count) {
        YearMonth first = last.minusMonths(count - 1L);
        Map<YearMonth, BigDecimal[]> sums = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            sums.put(first.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Transaction t : flows(user, first.atDay(1), last.atEndOfMonth())) {
            BigDecimal[] s = sums.get(YearMonth.from(t.getDate()));
            int i = t.getType() == TxnType.INCOME ? 0 : 1;
            s[i] = s[i].add(t.getAmount());
        }
        return sums.entrySet().stream().map(e -> new MonthSummary(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    public MonthSummary month(AppUser user, YearMonth month) {
        return months(user, month, 1).getFirst();
    }

    /** Average income and expense of the full months before {@code month} that have any activity (up to {@code count}). */
    public MonthSummary average(AppUser user, YearMonth month, int count) {
        List<MonthSummary> past = months(user, month.minusMonths(1), count).stream().filter(m -> !m.isEmpty()).toList();
        if (past.isEmpty()) {
            return new MonthSummary(month, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal n = BigDecimal.valueOf(past.size());
        return new MonthSummary(month,
                Money.sum(past, MonthSummary::income).divide(n, 2, java.math.RoundingMode.HALF_UP),
                Money.sum(past, MonthSummary::expense).divide(n, 2, java.math.RoundingMode.HALF_UP));
    }

    /** Totals per category for one type (INCOME or EXPENSE), biggest first. */
    public List<CategoryTotal> byCategory(List<Transaction> flows, TxnType type) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        Map<Long, Category> categories = new HashMap<>();
        for (Transaction t : flows) {
            if (t.getType() == type && t.getCategory() != null) {
                totals.merge(t.getCategory().getId(), t.getAmount(), BigDecimal::add);
                categories.putIfAbsent(t.getCategory().getId(), t.getCategory());
            }
        }
        BigDecimal all = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return totals.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    Category c = categories.get(e.getKey());
                    return new CategoryTotal(c.getId(), c.getName(), c.getColor(), e.getValue(), Money.percent(e.getValue(), all));
                })
                .toList();
    }

    /** Spent per category id. */
    public Map<Long, BigDecimal> spentByCategory(List<Transaction> flows) {
        return flows.stream().filter(t -> t.getType() == TxnType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.toMap(t -> t.getCategory().getId(), Transaction::getAmount, BigDecimal::add));
    }

    /** [needs, wants] for the expense lines given. */
    public BigDecimal[] needsAndWants(List<Transaction> flows) {
        BigDecimal needs = BigDecimal.ZERO, wants = BigDecimal.ZERO;
        for (Transaction t : flows) {
            if (t.getType() != TxnType.EXPENSE) continue;
            if (t.getCategory() != null && t.getCategory().isEssential()) needs = needs.add(t.getAmount());
            else wants = wants.add(t.getAmount());
        }
        return new BigDecimal[]{needs, wants};
    }

    /** Running total of spending for each day of the month up to {@code until}. */
    public List<BigDecimal> cumulativeSpend(List<Transaction> flows, YearMonth month, LocalDate until) {
        BigDecimal[] perDay = new BigDecimal[month.lengthOfMonth()];
        Arrays.fill(perDay, BigDecimal.ZERO);
        for (Transaction t : flows) {
            if (t.getType() == TxnType.EXPENSE && YearMonth.from(t.getDate()).equals(month)) {
                int d = t.getDate().getDayOfMonth() - 1;
                perDay[d] = perDay[d].add(t.getAmount());
            }
        }
        int lastDay = YearMonth.from(until).equals(month) ? until.getDayOfMonth() : month.lengthOfMonth();
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal run = BigDecimal.ZERO;
        for (int d = 0; d < lastDay; d++) {
            run = run.add(perDay[d]);
            out.add(run);
        }
        return out;
    }

    /** Largest single expenses, biggest first. */
    public List<Transaction> topExpenses(List<Transaction> flows, int limit) {
        return flows.stream().filter(t -> t.getType() == TxnType.EXPENSE)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed()).limit(limit).toList();
    }
}
