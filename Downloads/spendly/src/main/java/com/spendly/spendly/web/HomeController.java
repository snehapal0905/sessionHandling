package com.spendly.spendly.web;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.dto.PasswordChangeForm;
import com.spendly.spendly.dto.ProfileForm;
import com.spendly.spendly.dto.RegisterForm;
import com.spendly.spendly.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CurrentUser currentUser;
    private final DashboardService dashboardService;
    private final InsightService insightService;
    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/login")
    public String login() {
        return currentUser.find() != null ? "redirect:/dashboard" : "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (currentUser.find() != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            AppUser user = userService.register(form);
            ra.addFlashAttribute("success", "Welcome to Spendly, " + user.getFirstName() + "! Sign in to start planning.");
            ra.addFlashAttribute("username", user.getUsername());
            return "redirect:/login";
        } catch (BusinessException e) {
            result.rejectValue("username", "taken", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("d", dashboardService.build(currentUser.get(), Periods.today()));
        return "dashboard";
    }

    @GetMapping("/insights")
    public String insights(Model model) {
        AppUser user = currentUser.get();
        var today = Periods.today();
        var forecast = insightService.forecast(user, today);
        var month = java.time.YearMonth.from(today);
        model.addAttribute("f", forecast);
        model.addAttribute("insights", insightService.insights(user, today));
        model.addAttribute("month", month);
        model.addAttribute("pace", analyticsService.cumulativeSpend(analyticsService.flows(user, month), month, today));
        model.addAttribute("usual", analyticsService.average(user, month, 3));
        return "insights";
    }

    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("h", insightService.health(currentUser.get(), Periods.today()));
        return "health";
    }

    @GetMapping("/account")
    public String account(Model model) {
        AppUser user = currentUser.get();
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            form.setCurrencySymbol(user.getCurrencySymbol());
            form.setSavingsTargetPercent(user.getSavingsTargetPercent());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "account";
    }

    @PostMapping("/account/profile")
    public String saveProfile(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "account";
        }
        userService.updateProfile(form);
        ra.addFlashAttribute("success", "Profile saved");
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult result,
                                 Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return account(model);
        }
        userService.changeOwnPassword(form);
        ra.addFlashAttribute("success", "Password changed");
        return "redirect:/account";
    }
}
