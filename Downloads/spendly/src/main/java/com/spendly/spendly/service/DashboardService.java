package com.spendly.spendly.service;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.SavingsGoal;
import com.spendly.spendly.domain.Transaction;
import com.spendly.spendly.domain.TxnType;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.Views.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/** Everything the dashboard needs to answer: how much do I have, where did it go, am I on budget, what can I spend? */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountService accountService;
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final RecurringService recurringService;
    private final InsightService insightService;
    private final GoalService goalService;
    private final TransactionRepository transactionRepository;

    public record Dashboard(String greeting, YearMonth month, Balances balances, MonthSummary thisMonth, MonthSummary lastMonth,
                            List<MonthSummary> trend, List<CategoryTotal> spending, BudgetMonth budgets,
                            List<Transaction> recent, List<Occurrence> upcoming, Forecast forecast, List<Insight> insights,
                            List<SavingsGoal> goals, boolean empty) {

        public int getIncomeChange() {
            return change(thisMonth.income(), lastMonth.income());
        }

        public int getExpenseChange() {
            return change(thisMonth.expense(), lastMonth.expense());
        }

        public int getExpenseShare() {
            return Money.percent(thisMonth.expense(), thisMonth.income());
        }

        private static int change(java.math.BigDecimal now, java.math.BigDecimal before) {
            return before.signum() == 0 ? 0 : Money.percent(now.subtract(before), before);
        }
    }

    public Dashboard build(AppUser user, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        List<MonthSummary> trend = analyticsService.months(user, month, 6);
        List<Transaction> flows = analyticsService.flows(user, month);
        List<Insight> insights = insightService.insights(user, today);
        return new Dashboard(
                greeting(LocalTime.now()),
                month,
                accountService.balances(user),
                trend.getLast(),
                trend.get(trend.size() - 2),
                trend,
                analyticsService.byCategory(flows, TxnType.EXPENSE),
                budgetService.month(user, month, today),
                transactionRepository.findTop8ByUserOrderByDateDescIdDesc(user),
                recurringService.occurrences(user, today.plusDays(14)).stream().limit(6).toList(),
                insightService.forecast(user, today),
                insights.stream().filter(i -> i.severity() != Severity.INFO).limit(4).toList(),
                goalService.all(user).stream().filter(g -> !g.isComplete()).limit(3).toList(),
                transactionRepository.countByUser(user) == 0);
    }

    static String greeting(LocalTime now) {
        if (now.isBefore(LocalTime.NOON)) return "Good morning";
        if (now.isBefore(LocalTime.of(17, 0))) return "Good afternoon";
        return "Good evening";
    }
}
