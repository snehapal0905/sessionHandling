package com.spendly.spendly.web;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.service.AnalyticsService;
import com.spendly.spendly.service.BudgetService;
import com.spendly.spendly.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private static final String PREFIX = "budget_";

    private final CurrentUser currentUser;
    private final BudgetService budgetService;
    private final AnalyticsService analyticsService;

    @GetMapping
    public String list(@RequestParam(required = false) String month, Model model) {
        AppUser user = currentUser.get();
        YearMonth m = Periods.month(month);
        model.addAttribute("b", budgetService.month(user, m, Periods.today()));
        model.addAttribute("summary", analyticsService.month(user, m));
        model.addAttribute("month", m);
        model.addAttribute("isCurrent", m.equals(YearMonth.now()));
        return "budgets/list";
    }

    /** Inputs are named budget_{categoryId}. */
    @PostMapping("/save")
    public String save(@RequestParam String month, @RequestParam Map<String, String> params, RedirectAttributes ra) {
        YearMonth m = Periods.month(month);
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith(PREFIX)) {
                amounts.put(Long.valueOf(k.substring(PREFIX.length())), parse(v));
            }
        });
        budgetService.save(currentUser.get(), m, amounts);
        ra.addFlashAttribute("success", "Budgets for " + m.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " saved");
        return "redirect:/budgets?month=" + m;
    }

    @PostMapping("/copy")
    public String copy(@RequestParam String month, RedirectAttributes ra) {
        YearMonth m = Periods.month(month);
        int n = budgetService.copyPrevious(currentUser.get(), m);
        ra.addFlashAttribute(n > 0 ? "success" : "error", n > 0 ? "Copied " + n + " budgets from last month" : "Nothing to copy - last month had no budgets for the categories still open here");
        return "redirect:/budgets?month=" + m;
    }

    @PostMapping("/suggest")
    public String suggest(@RequestParam String month, RedirectAttributes ra) {
        YearMonth m = Periods.month(month);
        int n = budgetService.suggest(currentUser.get(), m);
        ra.addFlashAttribute(n > 0 ? "success" : "error", n > 0 ? "Suggested " + n + " budgets from your last three months of spending - adjust and save"
                : "No suggestions - every category with recent spending already has a budget");
        return "redirect:/budgets?month=" + m;
    }

    private static BigDecimal parse(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return new BigDecimal(v.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
