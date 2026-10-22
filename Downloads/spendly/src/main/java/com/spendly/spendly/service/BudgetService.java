package com.spendly.spendly.service;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.repository.BudgetRepository;
import com.spendly.spendly.service.Views.BudgetLine;
import com.spendly.spendly.service.Views.BudgetMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final BigDecimal ROUND_TO = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final AnalyticsService analyticsService;

    /**
     * Budget vs actual for every active expense category in the month, plus any archived category that still had
     * spending. In the current month, {@code projected} extends the spending pace to the end of the month.
     */
    public BudgetMonth month(AppUser user, YearMonth month, LocalDate today) {
        Map<Long, Budget> budgets = budgetRepository.findByUserAndPeriod(user, month.atDay(1)).stream()
                .collect(Collectors.toMap(b -> b.getCategory().getId(), b -> b));
        List<Transaction> flows = analyticsService.flows(user, month);
        Map<Long, BigDecimal> spent = analyticsService.spentByCategory(flows);
        // only day-to-day spending is extended to month end; bills posted from recurring items happen once
        Map<Long, BigDecimal> variable = analyticsService.spentByCategory(flows.stream().filter(t -> t.getRecurringItemId() == null).toList());

        double paceFactor = YearMonth.from(today).equals(month) ? (double) month.lengthOfMonth() / today.getDayOfMonth() : 1.0;

        Map<Long, Category> categories = new LinkedHashMap<>();
        categoryService.active(user, CategoryKind.EXPENSE).forEach(c -> categories.put(c.getId(), c));
        flows.stream().filter(t -> t.getType() == TxnType.EXPENSE && t.getCategory() != null)
                .forEach(t -> categories.putIfAbsent(t.getCategory().getId(), t.getCategory()));
        budgets.values().forEach(b -> categories.putIfAbsent(b.getCategory().getId(), b.getCategory()));

        List<BudgetLine> lines = categories.values().stream()
                .map(c -> {
                    BigDecimal s = spent.getOrDefault(c.getId(), BigDecimal.ZERO);
                    Budget b = budgets.get(c.getId());
                    BigDecimal v = variable.getOrDefault(c.getId(), BigDecimal.ZERO);
                    return new BudgetLine(c, b == null ? null : b.getAmount(), s, s.add(Money.times(v, paceFactor - 1)));
                })
                .sorted(Comparator.comparing(BudgetLine::isBudgeted).reversed()
                        .thenComparing(Comparator.comparing(BudgetLine::getPercent).reversed())
                        .thenComparing(l -> l.category().getName()))
                .toList();
        BigDecimal total = Money.sum(lines.stream().filter(BudgetLine::isBudgeted).toList(), BudgetLine::budget);
        BigDecimal budgetedSpent = Money.sum(lines.stream().filter(BudgetLine::isBudgeted).toList(), BudgetLine::spent);
        BigDecimal unbudgeted = Money.sum(lines.stream().filter(l -> !l.isBudgeted()).toList(), BudgetLine::spent);
        return new BudgetMonth(month, lines, total, budgetedSpent, unbudgeted);
    }

    /** Saves the amounts typed into the budget table; blank or zero removes that category's budget. */
    @Transactional
    public void save(AppUser user, YearMonth month, Map<Long, BigDecimal> amounts) {
        Map<Long, Budget> existing = budgetRepository.findByUserAndPeriod(user, month.atDay(1)).stream()
                .collect(Collectors.toMap(b -> b.getCategory().getId(), b -> b));
        amounts.forEach((categoryId, amount) -> {
            Budget b = existing.get(categoryId);
            if (amount == null || amount.signum() <= 0) {
                if (b != null) budgetRepository.delete(b);
                return;
            }
            if (b == null) {
                b = new Budget();
                b.setUser(user);
                b.setCategory(categoryService.get(user, categoryId));
                b.setPeriod(month.atDay(1));
            }
            b.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            budgetRepository.save(b);
        });
    }

    /** Copies last month's budgets into categories that have none yet. Returns how many were added. */
    @Transactional
    public int copyPrevious(AppUser user, YearMonth month) {
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        Set<Long> already = budgetRepository.findByUserAndPeriod(user, month.atDay(1)).stream()
                .map(b -> b.getCategory().getId()).collect(Collectors.toSet());
        for (Budget b : budgetRepository.findByUserAndPeriod(user, month.minusMonths(1).atDay(1))) {
            if (!already.contains(b.getCategory().getId()) && b.getCategory().isActive()) {
                amounts.put(b.getCategory().getId(), b.getAmount());
            }
        }
        save(user, month, amounts);
        return amounts.size();
    }

    /** Suggests a budget for unbudgeted categories: the average of the last three months, rounded up to 100. */
    @Transactional
    public int suggest(AppUser user, YearMonth month) {
        Set<Long> already = budgetRepository.findByUserAndPeriod(user, month.atDay(1)).stream()
                .map(b -> b.getCategory().getId()).collect(Collectors.toSet());
        List<Transaction> history = analyticsService.flows(user, month.minusMonths(3).atDay(1), month.minusMonths(1).atEndOfMonth());
        Map<Long, BigDecimal> spent = analyticsService.spentByCategory(history);
        long activeMonths = history.stream().map(t -> YearMonth.from(t.getDate())).distinct().count();
        if (activeMonths == 0) {
            return 0;
        }
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        for (Category c : categoryService.active(user, CategoryKind.EXPENSE)) {
            BigDecimal total = spent.get(c.getId());
            if (already.contains(c.getId()) || total == null) continue;
            BigDecimal avg = total.divide(BigDecimal.valueOf(activeMonths), 0, RoundingMode.UP);
            amounts.put(c.getId(), avg.divide(ROUND_TO, 0, RoundingMode.UP).multiply(ROUND_TO));
        }
        save(user, month, amounts);
        return amounts.size();
    }
}
