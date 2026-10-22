package com.spendly.spendly.service;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.CategoryKind;
import com.spendly.spendly.dto.CategoryForm;
import com.spendly.spendly.repository.BudgetRepository;
import com.spendly.spendly.repository.CategoryRepository;
import com.spendly.spendly.repository.RecurringItemRepository;
import com.spendly.spendly.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringItemRepository recurringRepository;
    private final BudgetRepository budgetRepository;

    public Category get(AppUser user, Long id) {
        return categoryRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException("Category", id));
    }

    public List<Category> all(AppUser user) {
        return categoryRepository.findByUserOrderByKindAscActiveDescNameAsc(user);
    }

    public List<Category> active(AppUser user, CategoryKind kind) {
        return categoryRepository.findByUserAndKindAndActiveTrueOrderByNameAsc(user, kind);
    }

    @Transactional
    public Category save(AppUser user, CategoryForm form) {
        Category c = form.isNew() ? new Category() : get(user, form.getId());
        String name = form.getName().trim();
        boolean renamed = form.isNew() || !c.getName().equalsIgnoreCase(name) || c.getKind() != form.getKind();
        if (renamed && categoryRepository.existsByUserAndKindAndNameIgnoreCase(user, form.getKind(), name)) {
            throw new BusinessException("You already have a " + form.getKind().getLabel().toLowerCase() + " category called \"" + name + "\"");
        }
        if (!form.isNew() && c.getKind() != form.getKind() && transactionRepository.existsByCategory(c)) {
            throw new BusinessException("\"" + c.getName() + "\" already has transactions, so it cannot switch between income and expense");
        }
        c.setUser(user);
        c.setName(name);
        c.setKind(form.getKind());
        c.setColor(form.getColor().toLowerCase());
        c.setEssential(form.getKind() == CategoryKind.EXPENSE && form.isEssential());
        c.setActive(form.isActive());
        return categoryRepository.save(c);
    }

    /** Used categories are archived rather than deleted so history keeps its labels. */
    @Transactional
    public boolean deleteOrArchive(AppUser user, Long id) {
        Category c = get(user, id);
        if (transactionRepository.existsByCategory(c) || recurringRepository.existsByCategory(c)) {
            c.setActive(false);
            return false;
        }
        budgetRepository.deleteByCategory(c);
        categoryRepository.delete(c);
        return true;
    }
}
