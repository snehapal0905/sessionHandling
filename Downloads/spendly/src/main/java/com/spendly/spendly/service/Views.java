package com.spendly.spendly.service;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.RecurringItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Read-only value objects handed to the templates. */
public final class Views {

    private Views() {
    }

    public record AccountBalance(Account account, BigDecimal balance) {
    }

    public record Balances(List<AccountBalance> accounts, BigDecimal total, BigDecimal liquid, BigDecimal creditOwed) {
    }

    public record MonthSummary(YearMonth month, BigDecimal income, BigDecimal expense) {
        public BigDecimal getNet() {
            return income.subtract(expense);
        }

        /** Share of income kept, in whole percent (negative when spending more than earning). */
        public int getSavingsRate() {
            return Money.percent(getNet(), income);
        }

        public String getLabel() {
            return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }

        public boolean isEmpty() {
            return income.signum() == 0 && expense.signum() == 0;
        }
    }

    public record CategoryTotal(Long id, String name, String color, BigDecimal amount, int share) {
    }

    public record BudgetLine(Category category, BigDecimal budget, BigDecimal spent, BigDecimal projected) {
        public boolean isBudgeted() {
            return budget != null && budget.signum() > 0;
        }

        public int getPercent() {
            return isBudgeted() ? Money.percent(spent, budget) : 0;
        }

        public BigDecimal getRemaining() {
            return isBudgeted() ? budget.subtract(spent) : BigDecimal.ZERO;
        }

        /** over / risk (projected to overshoot) / warn (80%+) / ok / none. */
        public String getStatus() {
            if (!isBudgeted()) return "none";
            if (spent.compareTo(budget) > 0) return "over";
            if (projected != null && projected.compareTo(budget) > 0) return "risk";
            if (getPercent() >= 80) return "warn";
            return "ok";
        }
    }

    public record BudgetMonth(YearMonth month, List<BudgetLine> lines, BigDecimal totalBudget, BigDecimal budgetedSpent,
                              BigDecimal unbudgetedSpent) {
        public int getPercent() {
            return Money.percent(budgetedSpent, totalBudget);
        }

        public BigDecimal getTotalSpent() {
            return budgetedSpent.add(unbudgetedSpent);
        }

        public List<BudgetLine> getBudgeted() {
            return lines.stream().filter(BudgetLine::isBudgeted).toList();
        }

        public long getOverCount() {
            return lines.stream().filter(l -> "over".equals(l.getStatus())).count();
        }
    }

    public record Occurrence(RecurringItem item, LocalDate date) {
        public boolean isOverdue() {
            return date.isBefore(LocalDate.now());
        }

        public long getDaysAway() {
            return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        }
    }

    public record Forecast(LocalDate today, int daysElapsed, int daysInMonth,
                           BigDecimal incomeSoFar, BigDecimal expenseSoFar,
                           BigDecimal pendingIncome, BigDecimal pendingBills, BigDecimal variableToCome,
                           BigDecimal dailyRate, BigDecimal balanceNow, BigDecimal savingsTarget) {

        public int getDaysLeft() {
            return daysInMonth - daysElapsed;
        }

        public BigDecimal getProjectedIncome() {
            return incomeSoFar.add(pendingIncome);
        }

        public BigDecimal getProjectedExpense() {
            return expenseSoFar.add(pendingBills).add(variableToCome);
        }

        public BigDecimal getProjectedNet() {
            return getProjectedIncome().subtract(getProjectedExpense());
        }

        public int getProjectedSavingsRate() {
            return Money.percent(getProjectedNet(), getProjectedIncome());
        }

        public BigDecimal getMonthEndBalance() {
            return balanceNow.add(pendingIncome).subtract(pendingBills).subtract(variableToCome);
        }

        /** Left for day-to-day spending after bills still due and the savings target. */
        public BigDecimal getSafeToSpend() {
            return getProjectedIncome().subtract(expenseSoFar).subtract(pendingBills).subtract(savingsTarget).max(BigDecimal.ZERO);
        }

        public BigDecimal getSafePerDay() {
            return getSafeToSpend().divide(BigDecimal.valueOf(getDaysLeft() + 1L), 0, java.math.RoundingMode.DOWN);
        }

        public int getMonthProgress() {
            return daysElapsed * 100 / daysInMonth;
        }
    }

    public enum Severity {
        CRITICAL, WARNING, INFO, GOOD;

        public boolean isAlert() {
            return this == CRITICAL || this == WARNING;
        }
    }

    public record Insight(Severity severity, String type, String title, String message, String link, String linkLabel) {
    }

    public record HealthFactor(String name, int score, int max, String value, String advice) {
        public int getPercent() {
            return max == 0 ? 0 : score * 100 / max;
        }
    }

    public record HealthScore(int score, List<HealthFactor> factors, BigDecimal avgIncome, BigDecimal avgExpense) {
        public String getGrade() {
            if (score >= 80) return "Excellent";
            if (score >= 65) return "Good";
            if (score >= 45) return "Fair";
            return "Needs attention";
        }
    }
}
