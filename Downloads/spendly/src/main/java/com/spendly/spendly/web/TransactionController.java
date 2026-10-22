package com.spendly.spendly.web;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.TransactionForm;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final CurrentUser currentUser;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String month, @RequestParam(required = false) TxnType type,
                       @RequestParam(required = false) Long account, @RequestParam(required = false) Long category,
                       @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        AppUser user = currentUser.get();
        YearMonth m = Periods.month(month);
        model.addAttribute("page", transactionRepository.search(user, m.atDay(1), m.atEndOfMonth(), type, account, category,
                Periods.q(q), Periods.page(page, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")))));
        List<Transaction> all = transactionRepository.searchAll(user, m.atDay(1), m.atEndOfMonth(), type, account, category, Periods.q(q));
        model.addAttribute("totalIncome", sum(all, TxnType.INCOME));
        model.addAttribute("totalExpense", sum(all, TxnType.EXPENSE));
        model.addAttribute("month", m);
        model.addAttribute("type", type);
        model.addAttribute("account", account);
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("types", TxnType.values());
        model.addAttribute("accounts", accountService.balances(user).accounts().stream().map(b -> b.account()).toList());
        model.addAttribute("categories", categoryService.all(user));
        return "transactions/list";
    }

    @GetMapping("/new")
    public String create(@RequestParam(defaultValue = "EXPENSE") TxnType type, @RequestParam(required = false) Long category, Model model) {
        TransactionForm form = new TransactionForm();
        form.setType(type);
        form.setCategoryId(category);
        List<Account> accounts = accountService.active(currentUser.get());
        if (!accounts.isEmpty()) {
            form.setAccountId(accounts.getFirst().getId());
        }
        model.addAttribute("form", form);
        return form(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", TransactionForm.of(transactionService.get(currentUser.get(), id)));
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") TransactionForm form, BindingResult result, Model model, RedirectAttributes ra,
                       @RequestParam(required = false) String another) {
        if (result.hasErrors()) {
            return form(model);
        }
        boolean isNew = form.isNew();
        Transaction t = transactionService.save(currentUser.get(), form);
        ra.addFlashAttribute("success", (isNew ? "Added " : "Updated ") + t.getType().getLabel().toLowerCase() + " \"" + t.getDescription() + "\"");
        if (another != null) {
            return "redirect:/transactions/new?type=" + t.getType();
        }
        return "redirect:/transactions?month=" + YearMonth.from(t.getDate());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Transaction t = transactionService.get(currentUser.get(), id);
        YearMonth m = YearMonth.from(t.getDate());
        transactionService.delete(currentUser.get(), id);
        ra.addFlashAttribute("success", "Deleted \"" + t.getDescription() + "\"");
        return "redirect:/transactions?month=" + m;
    }

    /** The filtered month as a CSV file, ready for a spreadsheet. */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String month, @RequestParam(required = false) TxnType type,
                       @RequestParam(required = false) Long account, @RequestParam(required = false) Long category,
                       @RequestParam(required = false) String q, HttpServletResponse response) throws IOException {
        AppUser user = currentUser.get();
        YearMonth m = Periods.month(month);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"spendly-" + m + ".csv\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter out = response.getWriter();
        out.print('﻿'); // lets Excel detect UTF-8 (₹ and other symbols)
        out.println("Date,Type,Description,Category,Account,To account,Amount,Notes");
        for (Transaction t : transactionRepository.searchAll(user, m.atDay(1), m.atEndOfMonth(), type, account, category, Periods.q(q))) {
            out.println(String.join(",", t.getDate().toString(), t.getType().getLabel(), csv(t.getDescription()),
                    csv(t.getCategory() == null ? "" : t.getCategory().getName()), csv(t.getAccount().getName()),
                    csv(t.getToAccount() == null ? "" : t.getToAccount().getName()), t.getAmount().toPlainString(), csv(t.getNotes())));
        }
        out.flush();
    }

    private String form(Model model) {
        AppUser user = currentUser.get();
        model.addAttribute("accounts", accountService.active(user));
        model.addAttribute("incomeCategories", categoryService.active(user, CategoryKind.INCOME));
        model.addAttribute("expenseCategories", categoryService.active(user, CategoryKind.EXPENSE));
        model.addAttribute("types", TxnType.values());
        return "transactions/form";
    }

    private static BigDecimal sum(List<Transaction> list, TxnType type) {
        return Money.sum(list.stream().filter(t -> t.getType() == type).toList(), Transaction::getAmount);
    }

    private static String csv(String s) {
        if (s == null) return "";
        // neutralise spreadsheet formulas and quote everything that needs it
        String v = s.matches("^[=+\\-@].*") ? "'" + s : s;
        return v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
