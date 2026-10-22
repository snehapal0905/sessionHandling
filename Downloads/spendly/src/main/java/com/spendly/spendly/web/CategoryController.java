package com.spendly.spendly.web;

import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.CategoryKind;
import com.spendly.spendly.dto.CategoryForm;
import com.spendly.spendly.service.CategoryService;
import com.spendly.spendly.service.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CurrentUser currentUser;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        List<Category> all = categoryService.all(currentUser.get());
        model.addAttribute("expense", all.stream().filter(c -> c.getKind() == CategoryKind.EXPENSE).toList());
        model.addAttribute("income", all.stream().filter(c -> c.getKind() == CategoryKind.INCOME).toList());
        return "categories/list";
    }

    @GetMapping("/new")
    public String create(@RequestParam(defaultValue = "EXPENSE") CategoryKind kind, Model model) {
        CategoryForm form = new CategoryForm();
        form.setKind(kind);
        model.addAttribute("form", form);
        return form(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", CategoryForm.of(categoryService.get(currentUser.get(), id)));
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") CategoryForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return form(model);
        }
        Category c = categoryService.save(currentUser.get(), form);
        ra.addFlashAttribute("success", "Saved category \"" + c.getName() + "\"");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = categoryService.deleteOrArchive(currentUser.get(), id);
        ra.addFlashAttribute("success", deleted ? "Category deleted" : "Category archived - it has transactions, so its history is kept");
        return "redirect:/categories";
    }

    private String form(Model model) {
        model.addAttribute("kinds", CategoryKind.values());
        return "categories/form";
    }
}
