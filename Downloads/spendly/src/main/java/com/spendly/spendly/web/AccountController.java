package com.spendly.spendly.web;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.AccountType;
import com.spendly.spendly.dto.AccountForm;
import com.spendly.spendly.service.AccountService;
import com.spendly.spendly.service.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CurrentUser currentUser;
    private final AccountService accountService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("b", accountService.balances(currentUser.get()));
        return "accounts/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("form", new AccountForm());
        return form(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", AccountForm.of(accountService.get(currentUser.get(), id)));
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") AccountForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return form(model);
        }
        Account a = accountService.save(currentUser.get(), form);
        ra.addFlashAttribute("success", "Saved account \"" + a.getName() + "\"");
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        Account a = accountService.toggleActive(currentUser.get(), id);
        ra.addFlashAttribute("success", (a.isActive() ? "Restored \"" : "Archived \"") + a.getName() + "\"");
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        accountService.delete(currentUser.get(), id);
        ra.addFlashAttribute("success", "Account deleted");
        return "redirect:/accounts";
    }

    private String form(Model model) {
        model.addAttribute("types", AccountType.values());
        return "accounts/form";
    }
}
