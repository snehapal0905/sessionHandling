package com.spendly.spendly.service;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.service.Views.*;
import com.spendly.spendly.web.Fmt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Predicts the month end, spots overspending risks and scores overall financial health. */
@Service
@RequiredArgsConstructor
public class InsightService {

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");

    private final AnalyticsService analyticsService;
    private final AccountService accountService;
    private final BudgetService budgetService;
    private final RecurringService recurringService;
    private final GoalService goalService;
    private final Fmt fmt;

    /**
     * Month-end forecast. Bills and income still due come from recurring items; day-to-day spending for the rest of
     * the month uses the current pace, blended with the last three months early in the month when there is little data.
     */
    public Forecast forecast(AppUser user, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        List<Transaction> flows = analyticsService.flows(user, month);
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO, variable = BigDecimal.ZERO;
        for (Transaction t : flows) {
            if (t.getType() == TxnType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
                if (t.getRecurringItemId() == null) variable = variable.add(t.getAmount());
            }
        }
        List<Occurrence> due = recurringService.occurrences(user, month.atEndOfMonth());

        int day = today.getDayOfMonth(), days = month.lengthOfMonth();
        double currentRate = variable.doubleValue() / day;
        double rate = currentRate;
        double historic = historicDailyVariableSpend(user, month);
        if (historic > 0) {
            double weight = (double) day / days;
            rate = weight * currentRate + (1 - weight) * historic;
        }
        BigDecimal pendingIncome = recurringService.total(due, TxnType.INCOME);
        BigDecimal savingsTarget = income.add(pendingIncome)
                .multiply(BigDecimal.valueOf(user.getSavingsTargetPercent())).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new Forecast(today, day, days, income, expense, pendingIncome, recurringService.total(due, TxnType.EXPENSE),
                Money.of(rate * (days - day)), Money.of(rate), accountService.balances(user).total(), savingsTarget);
    }

    private double historicDailyVariableSpend(AppUser user, YearMonth month) {
        YearMonth from = month.minusMonths(3), to = month.minusMonths(1);
        List<Transaction> history = analyticsService.flows(user, from.atDay(1), to.atEndOfMonth());
        Set<YearMonth> active = new HashSet<>();
        double total = 0;
        for (Transaction t : history) {
            active.add(YearMonth.from(t.getDate()));
            if (t.getType() == TxnType.EXPENSE && t.getRecurringItemId() == null) total += t.getAmount().doubleValue();
        }
        int days = active.stream().mapToInt(YearMonth::lengthOfMonth).sum();
        return days == 0 ? 0 : total / days;
    }

    /** Risks first (critical, then warnings), then tips and good news. */
    public List<Insight> insights(AppUser user, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        List<Insight> out = new ArrayList<>();
        Forecast f = forecast(user, today);
        BudgetMonth budgets = budgetService.month(user, month, today);

        // ---- budgets
        for (BudgetLine l : budgets.lines()) {
            String name = l.category().getName();
            switch (l.getStatus()) {
                case "over" -> out.add(new Insight(Severity.CRITICAL, "Budget", name + " is over budget",
                        fmt.whole(l.spent()) + " spent of " + fmt.whole(l.budget()) + " (" + l.getPercent() + "%). "
                                + fmt.whole(l.spent().subtract(l.budget())) + " over.", "/budgets", "Review budget"));
                case "risk" -> out.add(new Insight(Severity.WARNING, "Budget", name + " may overshoot",
                        "At this pace you will spend about " + fmt.whole(l.projected()) + " against a " + fmt.whole(l.budget())
                                + " budget. Keep to about " + fmt.whole(l.getRemaining().divide(BigDecimal.valueOf(f.getDaysLeft() + 1L), 0, RoundingMode.DOWN))
                                + " a day for the rest of the month to stay inside it.", "/budgets", "Review budget"));
                default -> {
                }
            }
        }
        if (budgets.getBudgeted().isEmpty()) {
            out.add(new Insight(Severity.INFO, "Planning", "No budgets for " + month.format(MONTH_YEAR) + " yet",
                    "Set a limit for your main spending categories, or let Spendly suggest them from the last three months.",
                    "/budgets", "Plan budgets"));
        }

        // ---- the month as a whole
        if (f.getProjectedIncome().signum() > 0 && f.getProjectedNet().signum() < 0) {
            out.add(new Insight(Severity.CRITICAL, "Forecast", "On track to spend more than you earn",
                    "Projected spending of " + fmt.whole(f.getProjectedExpense()) + " is above projected income of "
                            + fmt.whole(f.getProjectedIncome()) + " this month.", "/insights", "See forecast"));
        } else if (f.getProjectedIncome().signum() > 0 && f.getProjectedSavingsRate() < user.getSavingsTargetPercent()) {
            out.add(new Insight(Severity.WARNING, "Savings", "Savings below your " + user.getSavingsTargetPercent() + "% target",
                    "You are on track to keep " + f.getProjectedSavingsRate() + "% of your income (" + fmt.whole(f.getProjectedNet()) + ") this month.",
                    "/insights", "See forecast"));
        } else if (f.getProjectedIncome().signum() > 0) {
            out.add(new Insight(Severity.GOOD, "Savings", "On track to save " + fmt.whole(f.getProjectedNet()),
                    "That is " + f.getProjectedSavingsRate() + "% of this month's income, against your "
                            + user.getSavingsTargetPercent() + "% target.", "/insights", "See forecast"));
        }
        if (f.getMonthEndBalance().signum() < 0) {
            out.add(new Insight(Severity.CRITICAL, "Forecast", "Balance may go negative",
                    "Your accounts are projected to end the month at " + fmt.whole(f.getMonthEndBalance()) + ".", "/accounts", "Accounts"));
        }

        // ---- bills
        List<Occurrence> upcoming = recurringService.occurrences(user, today.plusDays(7));
        List<Occurrence> overdue = upcoming.stream().filter(o -> o.date().isBefore(today)).toList();
        List<Occurrence> thisWeek = upcoming.stream().filter(o -> !o.date().isBefore(today) && o.item().getType() == TxnType.EXPENSE).toList();
        if (!overdue.isEmpty()) {
            out.add(new Insight(Severity.WARNING, "Bills", overdue.size() + (overdue.size() == 1 ? " recurring item is" : " recurring items are") + " overdue",
                    names(overdue) + ". Post them once paid, or skip if they did not happen.", "/recurring", "Recurring"));
        }
        if (!thisWeek.isEmpty()) {
            out.add(new Insight(Severity.INFO, "Bills", thisWeek.size() + (thisWeek.size() == 1 ? " bill" : " bills") + " due in the next 7 days",
                    names(thisWeek) + " - " + fmt.whole(recurringService.total(thisWeek, TxnType.EXPENSE)) + " in total.", "/recurring", "Recurring"));
        }

        // ---- spending spikes against the usual
        Map<Long, BigDecimal> history = analyticsService.spentByCategory(
                analyticsService.flows(user, month.minusMonths(3).atDay(1), month.minusMonths(1).atEndOfMonth()));
        for (BudgetLine l : budgets.lines()) {
            BigDecimal usual = history.getOrDefault(l.category().getId(), BigDecimal.ZERO).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            BigDecimal pace = l.projected();
            if (usual.compareTo(BigDecimal.valueOf(500)) > 0 && pace.compareTo(usual.multiply(BigDecimal.valueOf(1.3))) > 0
                    && !"over".equals(l.getStatus()) && !"risk".equals(l.getStatus()) && f.daysElapsed() >= 7) {
                out.add(new Insight(Severity.WARNING, "Trend", l.category().getName() + " is up " + (Money.percent(pace, usual) - 100) + "% on usual",
                        "Heading for about " + fmt.whole(pace) + " this month against a three-month average of " + fmt.whole(usual) + ".",
                        "/transactions?category=" + l.category().getId(), "See transactions"));
            }
        }

        // ---- debt, goals and recurring costs
        Balances balances = accountService.balances(user);
        if (balances.creditOwed().signum() > 0) {
            out.add(new Insight(Severity.INFO, "Debt", fmt.whole(balances.creditOwed()) + " owed on credit cards",
                    "Pay the full statement before the due date to avoid interest.", "/accounts", "Accounts"));
        }
        for (SavingsGoal g : goalService.all(user)) {
            BigDecimal monthly = g.monthlyNeeded(today);
            if (!g.isComplete() && monthly != null) {
                out.add(new Insight(Severity.INFO, "Goal", "Put aside " + fmt.whole(monthly) + " a month for " + g.getName(),
                        "That reaches " + fmt.whole(g.getTargetAmount()) + " by " + g.getTargetDate().format(MONTH_YEAR)
                                + ". You have saved " + g.getPercent() + "% so far.", "/goals", "Goals"));
            }
        }
        BigDecimal fixed = recurringService.monthlyTotal(user, TxnType.EXPENSE);
        if (fixed.signum() > 0) {
            out.add(new Insight(Severity.INFO, "Optimize", "Recurring costs: " + fmt.whole(fixed) + " a month",
                    "That is " + fmt.whole(fixed.multiply(BigDecimal.valueOf(12))) + " a year. Cancel subscriptions you no longer use.",
                    "/recurring", "Review"));
        }

        out.sort(Comparator.comparing(Insight::severity));
        return out;
    }

    public long alertCount(AppUser user, LocalDate today) {
        return insights(user, today).stream().filter(i -> i.severity().isAlert()).count();
    }

    /**
     * Health score out of 100 from five factors: savings rate (30), budget discipline (20), emergency cushion (25),
     * debt load (10) and fixed costs (15). Uses the average of the last three months with activity.
     */
    public HealthScore health(AppUser user, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        MonthSummary avg = analyticsService.average(user, month, 3);
        if (avg.isEmpty()) {
            avg = analyticsService.month(user, month);
        }
        BigDecimal income = avg.income(), expense = avg.expense();
        List<HealthFactor> factors = new ArrayList<>();

        int target = Math.max(user.getSavingsTargetPercent(), 5);
        int rate = avg.getSavingsRate();
        factors.add(new HealthFactor("Savings rate", scale(rate, target, 30), 30, rate + "% of income kept",
                rate >= target ? "You save at or above your " + target + "% target. Keep it up."
                        : "Aim to keep " + target + "% of income. Trim one or two wants categories to close the gap."));

        int checked = 0, kept = 0;
        for (YearMonth m : List.of(month.minusMonths(1), month)) {
            for (BudgetLine l : budgetService.month(user, m, today).getBudgeted()) {
                checked++;
                if (l.spent().compareTo(l.budget()) <= 0) kept++;
            }
        }
        factors.add(new HealthFactor("Budget discipline", checked == 0 ? 0 : kept * 20 / checked, 20,
                checked == 0 ? "No budgets set" : kept + " of " + checked + " budgets kept",
                checked == 0 ? "Set monthly budgets so you can see where you stand."
                        : kept == checked ? "Every budget kept last month and this month." : "Look at the categories that went over and raise or rein them in."));

        Balances balances = accountService.balances(user);
        double cover = expense.signum() == 0 ? 0 : balances.liquid().doubleValue() / expense.doubleValue();
        factors.add(new HealthFactor("Emergency cushion", (int) Math.round(Math.max(0, Math.min(cover / 6, 1)) * 25), 25,
                String.format(Locale.ENGLISH, "%.1f months of spending", Math.max(cover, 0)),
                cover >= 6 ? "Six months or more set aside. Well covered."
                        : "Build cash and savings up to six months of spending (" + fmt.whole(expense.multiply(BigDecimal.valueOf(6))) + ")."));

        double debtRatio = income.signum() == 0 ? (balances.creditOwed().signum() > 0 ? 1 : 0)
                : balances.creditOwed().doubleValue() / income.doubleValue();
        factors.add(new HealthFactor("Debt load", (int) Math.round((1 - Math.min(debtRatio / 0.5, 1)) * 10), 10,
                fmt.whole(balances.creditOwed()) + " on cards (" + Math.round(debtRatio * 100) + "% of income)",
                debtRatio == 0 ? "No card debt. Nicely done." : "Clear card balances in full each month to keep this at zero."));

        BigDecimal fixed = recurringService.monthlyTotal(user, TxnType.EXPENSE);
        double fixedRatio = income.signum() == 0 ? 0 : fixed.doubleValue() / income.doubleValue();
        int fixedScore = fixedRatio <= 0.5 ? 15 : fixedRatio >= 0.8 ? 0 : (int) Math.round((0.8 - fixedRatio) / 0.3 * 15);
        factors.add(new HealthFactor("Fixed costs", income.signum() == 0 ? 0 : fixedScore, 15,
                Math.round(fixedRatio * 100) + "% of income is committed",
                fixedRatio <= 0.5 ? "Bills and subscriptions leave you plenty of room." : "Fixed bills take a big share. Review subscriptions and plans."));

        int score = factors.stream().mapToInt(HealthFactor::score).sum();
        return new HealthScore(score, factors, income, expense);
    }

    private static int scale(int value, int full, int max) {
        if (value <= 0) return 0;
        return Math.min(max, value * max / full);
    }

    private static String names(List<Occurrence> occurrences) {
        List<String> names = occurrences.stream().map(o -> o.item().getName()).distinct().limit(3).toList();
        String s = String.join(", ", names);
        return occurrences.size() > names.size() ? s + " and more" : s;
    }
}
