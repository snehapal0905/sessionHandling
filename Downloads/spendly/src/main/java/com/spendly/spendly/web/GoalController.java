package com.spendly.spendly.web;

import com.spendly.spendly.domain.SavingsGoal;
import com.spendly.spendly.dto.GoalForm;
import com.spendly.spendly.service.CurrentUser;
import com.spendly.spendly.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final CurrentUser currentUser;
    private final GoalService goalService;
    private final Fmt fmt;

    @GetMapping
    public String list(Model model) {
        List<SavingsGoal> goals = goalService.all(currentUser.get());
        model.addAttribute("goals", goals);
        model.addAttribute("today", Periods.today());
        model.addAttribute("totalSaved", goals.stream().map(SavingsGoal::getSavedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totalTarget", goals.stream().map(SavingsGoal::getTargetAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("monthlyNeeded", goals.stream().filter(g -> !g.isComplete())
                .map(g -> g.monthlyNeeded(Periods.today())).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "goals/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("form", new GoalForm());
        return "goals/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", GoalForm.of(goalService.get(currentUser.get(), id)));
        return "goals/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") GoalForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "goals/form";
        }
        SavingsGoal g = goalService.save(currentUser.get(), form);
        ra.addFlashAttribute("success", "Saved goal \"" + g.getName() + "\"");
        return "redirect:/goals";
    }

    @PostMapping("/{id}/contribute")
    public String contribute(@PathVariable Long id, @RequestParam(required = false) BigDecimal amount,
                             @RequestParam(defaultValue = "add") String action, RedirectAttributes ra) {
        BigDecimal signed = amount == null ? null : ("withdraw".equals(action) ? amount.abs().negate() : amount.abs());
        SavingsGoal g = goalService.contribute(currentUser.get(), id, signed);
        ra.addFlashAttribute("success", g.isComplete() ? "Goal reached - \"" + g.getName() + "\" is fully funded!"
                : ("withdraw".equals(action) ? "Took " : "Added ") + fmt.whole(amount.abs()) + ("withdraw".equals(action) ? " from \"" : " to \"")
                + g.getName() + "\" - " + g.getPercent() + "% there");
        return "redirect:/goals";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        goalService.delete(currentUser.get(), id);
        ra.addFlashAttribute("success", "Goal deleted");
        return "redirect:/goals";
    }
}
