package com.spendly.spendly.web;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Transaction;
import com.spendly.spendly.domain.TxnType;
import com.spendly.spendly.service.AnalyticsService;
import com.spendly.spendly.service.CurrentUser;
import com.spendly.spendly.service.Money;
import com.spendly.spendly.service.Views.CategoryTotal;
import com.spendly.spendly.service.Views.MonthSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CurrentUser currentUser;
    private final AnalyticsService analyticsService;

    /** A category's spending this month next to the month before (null when it had none). */
    public record CategoryRow(CategoryTotal c, BigDecimal prev) {
        public BigDecimal getDiff() {
            return prev == null ? null : c.amount().subtract(prev);
        }
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "6") int range, @RequestParam(required = false) String month, Model model) {
        AppUser user = currentUser.get();
        int months = range == 12 ? 12 : 6;
        YearMonth m = Periods.month(month);
        List<MonthSummary> trend = analyticsService.months(user, YearMonth.now(), months);
        BigDecimal income = Money.sum(trend, MonthSummary::income);
        BigDecimal expense = Money.sum(trend, MonthSummary::expense);

        List<Transaction> flows = analyticsService.flows(user, m);
        List<CategoryTotal> spending = analyticsService.byCategory(flows, TxnType.EXPENSE);
        Map<Long, BigDecimal> previous = analyticsService.byCategory(analyticsService.flows(user, m.minusMonths(1)), TxnType.EXPENSE)
                .stream().collect(Collectors.toMap(CategoryTotal::id, CategoryTotal::amount));
        BigDecimal[] needsWants = analyticsService.needsAndWants(flows);

        model.addAttribute("range", months);
        model.addAttribute("trend", trend);
        model.addAttribute("rangeIncome", income);
        model.addAttribute("rangeExpense", expense);
        model.addAttribute("rangeNet", income.subtract(expense));
        model.addAttribute("rangeRate", Money.percent(income.subtract(expense), income));
        model.addAttribute("month", m);
        model.addAttribute("summary", analyticsService.month(user, m));
        model.addAttribute("spending", spending.stream().map(c -> new CategoryRow(c, previous.get(c.id()))).toList());
        model.addAttribute("incomeSources", analyticsService.byCategory(flows, TxnType.INCOME));
        model.addAttribute("needs", needsWants[0]);
        model.addAttribute("wants", needsWants[1]);
        model.addAttribute("needsShare", Money.percent(needsWants[0], needsWants[0].add(needsWants[1])));
        model.addAttribute("topExpenses", analyticsService.topExpenses(flows, 8));
        return "reports/index";
    }
}
