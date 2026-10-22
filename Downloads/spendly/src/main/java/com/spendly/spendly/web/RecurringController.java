package com.spendly.spendly.web;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.RecurringForm;
import com.spendly.spendly.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recurring")
@RequiredArgsConstructor
public class RecurringController {

    private final CurrentUser currentUser;
    private final RecurringService recurringService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final Fmt fmt;

    @GetMapping
    public String list(Model model) {
        AppUser user = currentUser.get();
        model.addAttribute("items", recurringService.all(user));
        model.addAttribute("upcoming", recurringService.occurrences(user, Periods.today().plusDays(30)));
        model.addAttribute("monthlyBills", recurringService.monthlyTotal(user, TxnType.EXPENSE));
        model.addAttribute("monthlyIncome", recurringService.monthlyTotal(user, TxnType.INCOME));
        return "recurring/list";
    }

    @GetMapping("/new")
    public String create(@RequestParam(defaultValue = "EXPENSE") TxnType type, Model model) {
        RecurringForm form = new RecurringForm();
        form.setType(type == TxnType.TRANSFER ? TxnType.EXPENSE : type);
        model.addAttribute("form", form);
        return form(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", RecurringForm.of(recurringService.get(currentUser.get(), id)));
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") RecurringForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return form(model);
        }
        RecurringItem r = recurringService.save(currentUser.get(), form);
        ra.addFlashAttribute("success", "Saved \"" + r.getName() + "\"");
        return "redirect:/recurring";
    }

    @PostMapping("/{id}/post")
    public String post(@PathVariable Long id, RedirectAttributes ra, @RequestHeader(value = "Referer", required = false) String referer) {
        Transaction t = recurringService.post(currentUser.get(), id, Periods.today());
        ra.addFlashAttribute("success", "Recorded \"" + t.getDescription() + "\" - " + fmt.whole(t.getAmount()) + " on " + fmt.date(t.getDate()));
        return back(referer);
    }

    @PostMapping("/{id}/skip")
    public String skip(@PathVariable Long id, RedirectAttributes ra, @RequestHeader(value = "Referer", required = false) String referer) {
        recurringService.skip(currentUser.get(), id);
        ra.addFlashAttribute("success", "Skipped this time - moved to the next due date");
        return back(referer);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        RecurringItem r = recurringService.toggleActive(currentUser.get(), id);
        ra.addFlashAttribute("success", (r.isActive() ? "Resumed \"" : "Paused \"") + r.getName() + "\"");
        return "redirect:/recurring";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        recurringService.delete(currentUser.get(), id);
        ra.addFlashAttribute("success", "Recurring item deleted. Transactions already recorded are kept.");
        return "redirect:/recurring";
    }

    private String form(Model model) {
        AppUser user = currentUser.get();
        model.addAttribute("accounts", accountService.active(user));
        model.addAttribute("incomeCategories", categoryService.active(user, CategoryKind.INCOME));
        model.addAttribute("expenseCategories", categoryService.active(user, CategoryKind.EXPENSE));
        model.addAttribute("frequencies", Frequency.values());
        return "recurring/form";
    }

    /** Back to the dashboard when posted from there, otherwise to the recurring list. */
    private static String back(String referer) {
        return referer != null && referer.contains("/dashboard") ? "redirect:/dashboard" : "redirect:/recurring";
    }
}
