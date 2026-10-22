package com.spendly.spendly.service;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.RecurringForm;
import com.spendly.spendly.repository.RecurringItemRepository;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.Views.Occurrence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringService {

    private final RecurringItemRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public RecurringItem get(AppUser user, Long id) {
        return recurringRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException("Recurring item", id));
    }

    public List<RecurringItem> all(AppUser user) {
        return recurringRepository.findByUserOrderByActiveDescNextDueDateAsc(user);
    }

    /** Every unposted occurrence of active items up to and including {@code until}, earliest first (overdue ones included). */
    public List<Occurrence> occurrences(AppUser user, LocalDate until) {
        List<Occurrence> out = new ArrayList<>();
        for (RecurringItem item : recurringRepository.findByUserAndActiveTrue(user)) {
            LocalDate d = item.getNextDueDate();
            for (int guard = 0; !d.isAfter(until) && guard < 400; guard++) {
                out.add(new Occurrence(item, d));
                d = item.getFrequency().next(d);
            }
        }
        out.sort(Comparator.comparing(Occurrence::date).thenComparing(o -> o.item().getName()));
        return out;
    }

    public BigDecimal total(List<Occurrence> occurrences, TxnType type) {
        return Money.sum(occurrences.stream().filter(o -> o.item().getType() == type).toList(), o -> o.item().getAmount());
    }

    /** Average monthly cost (or income) of everything that repeats. */
    public BigDecimal monthlyTotal(AppUser user, TxnType type) {
        return Money.sum(recurringRepository.findByUserAndActiveTrue(user).stream().filter(r -> r.getType() == type).toList(),
                RecurringItem::getMonthlyAmount);
    }

    @Transactional
    public RecurringItem save(AppUser user, RecurringForm form) {
        RecurringItem r = form.isNew() ? new RecurringItem() : get(user, form.getId());
        Category category = categoryService.get(user, form.getCategoryId());
        CategoryKind expected = form.getType() == TxnType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
        if (category.getKind() != expected) {
            throw new BusinessException("Pick an " + expected.getLabel().toLowerCase() + " category for " + form.getType().getLabel().toLowerCase());
        }
        r.setUser(user);
        r.setName(form.getName().trim());
        r.setType(form.getType());
        r.setAccount(accountService.get(user, form.getAccountId()));
        r.setCategory(category);
        r.setAmount(form.getAmount());
        r.setFrequency(form.getFrequency());
        r.setNextDueDate(form.getNextDueDate());
        r.setActive(form.isActive());
        return recurringRepository.save(r);
    }

    /**
     * Records the next occurrence as a transaction and moves the due date on. An early payment is dated today,
     * an overdue one on its due date.
     */
    @Transactional
    public Transaction post(AppUser user, Long id, LocalDate today) {
        RecurringItem r = get(user, id);
        if (!r.isActive()) {
            throw new BusinessException("\"" + r.getName() + "\" is paused. Resume it first.");
        }
        Transaction t = new Transaction();
        t.setUser(user);
        t.setType(r.getType());
        t.setAccount(r.getAccount());
        t.setCategory(r.getCategory());
        t.setAmount(r.getAmount());
        t.setDate(r.getNextDueDate().isAfter(today) ? today : r.getNextDueDate());
        t.setDescription(r.getName());
        t.setRecurringItemId(r.getId());
        transactionRepository.save(t);
        r.setNextDueDate(r.getFrequency().next(r.getNextDueDate()));
        return t;
    }

    /** Moves the due date on without recording anything (e.g. a month the gym was frozen). */
    @Transactional
    public void skip(AppUser user, Long id) {
        RecurringItem r = get(user, id);
        r.setNextDueDate(r.getFrequency().next(r.getNextDueDate()));
    }

    @Transactional
    public RecurringItem toggleActive(AppUser user, Long id) {
        RecurringItem r = get(user, id);
        r.setActive(!r.isActive());
        return r;
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        recurringRepository.delete(get(user, id));
    }
}
