package com.spendly.spendly;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Frequency;
import com.spendly.spendly.domain.TxnType;
import com.spendly.spendly.repository.AppUserRepository;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.*;
import com.spendly.spendly.service.Views.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MoneyMathTests {

    @Autowired AppUserRepository userRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired AccountService accountService;
    @Autowired AnalyticsService analyticsService;
    @Autowired InsightService insightService;
    @Autowired BudgetService budgetService;

    @Test
    void balancesAddUpToOpeningPlusIncomeMinusExpenses() {
        AppUser demo = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
        Balances b = accountService.balances(demo);
        BigDecimal opening = Money.sum(b.accounts(), ab -> ab.account().getOpeningBalance());
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (var t : transactionRepository.findAll()) {
            if (!t.getUser().getId().equals(demo.getId())) continue;
            if (t.getType() == TxnType.INCOME) income = income.add(t.getAmount());
            if (t.getType() == TxnType.EXPENSE) expense = expense.add(t.getAmount());
        }
        // transfers move money between accounts and cancel out in the total
        assertThat(b.total()).isEqualByComparingTo(opening.add(income).subtract(expense));
    }

    @Test
    void monthSummariesExcludeTransfers() {
        AppUser demo = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
        MonthSummary last = analyticsService.month(demo, YearMonth.now().minusMonths(1));
        assertThat(last.income()).isGreaterThanOrEqualTo(BigDecimal.valueOf(55_000));
        assertThat(last.expense()).isPositive().isLessThan(last.income());
    }

    @Test
    void forecastAndHealthScoreAreSane() {
        AppUser demo = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
        Forecast f = insightService.forecast(demo, LocalDate.now());
        assertThat(f.getProjectedExpense()).isGreaterThanOrEqualTo(f.expenseSoFar());
        assertThat(f.getSafeToSpend()).isNotNegative();
        HealthScore h = insightService.health(demo, LocalDate.now());
        assertThat(h.score()).isBetween(0, 100);
        assertThat(h.factors()).hasSize(5);
    }

    @Test
    void demoShoppingIsFlaggedOverBudgetThisMonth() {
        AppUser demo = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() < 14) return; // the seeded over-spend lands on the 14th
        BudgetMonth b = budgetService.month(demo, YearMonth.from(today), today);
        assertThat(b.lines()).anySatisfy(l -> {
            assertThat(l.category().getName()).isEqualTo("Shopping");
            assertThat(l.getStatus()).isEqualTo("over");
        });
    }

    @Test
    void frequenciesConvertToMonthlyCost() {
        assertThat(Frequency.YEARLY.monthly(BigDecimal.valueOf(12_000))).isEqualByComparingTo("1000");
        assertThat(Frequency.WEEKLY.monthly(BigDecimal.valueOf(300))).isEqualByComparingTo("1300");
        assertThat(Frequency.QUARTERLY.next(LocalDate.of(2026, 1, 31))).isEqualTo(LocalDate.of(2026, 4, 30));
    }
}
