package com.spendly.spendly.service;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.TransactionForm;
import com.spendly.spendly.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public Transaction get(AppUser user, Long id) {
        return transactionRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException("Transaction", id));
    }

    @Transactional
    public Transaction save(AppUser user, TransactionForm form) {
        Transaction t = form.isNew() ? new Transaction() : get(user, form.getId());
        t.setUser(user);
        t.setType(form.getType());
        t.setAccount(accountService.get(user, form.getAccountId()));
        if (form.getType() == TxnType.TRANSFER) {
            t.setToAccount(accountService.get(user, form.getToAccountId()));
            t.setCategory(null);
        } else {
            Category category = categoryService.get(user, form.getCategoryId());
            CategoryKind expected = form.getType() == TxnType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
            if (category.getKind() != expected) {
                throw new BusinessException("\"" + category.getName() + "\" is an " + category.getKind().getLabel().toLowerCase()
                        + " category; pick an " + expected.getLabel().toLowerCase() + " category");
            }
            t.setCategory(category);
            t.setToAccount(null);
        }
        t.setAmount(form.getAmount());
        t.setDate(form.getDate());
        t.setDescription(form.getDescription().trim());
        t.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().trim());
        return transactionRepository.save(t);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        transactionRepository.delete(get(user, id));
    }
}
