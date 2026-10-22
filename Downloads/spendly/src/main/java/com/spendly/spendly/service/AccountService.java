package com.spendly.spendly.service;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.AccountType;
import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.TxnType;
import com.spendly.spendly.dto.AccountForm;
import com.spendly.spendly.repository.AccountRepository;
import com.spendly.spendly.repository.RecurringItemRepository;
import com.spendly.spendly.repository.TransactionRepository;
import com.spendly.spendly.service.Views.AccountBalance;
import com.spendly.spendly.service.Views.Balances;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringItemRepository recurringRepository;

    public Account get(AppUser user, Long id) {
        return accountRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException("Account", id));
    }

    public List<Account> active(AppUser user) {
        return accountRepository.findByUserAndActiveTrueOrderByNameAsc(user);
    }

    /**
     * Balance of every account: opening balance + income - expenses - transfers out + transfers in.
     * Totals only include active accounts; credit cards count as negative (money owed).
     */
    public Balances balances(AppUser user) {
        Map<Long, BigDecimal> movement = new HashMap<>();
        for (Object[] row : transactionRepository.sumByAccountAndType(user)) {
            BigDecimal amount = (BigDecimal) row[2];
            BigDecimal signed = row[1] == TxnType.INCOME ? amount : amount.negate();
            movement.merge((Long) row[0], signed, BigDecimal::add);
        }
        for (Object[] row : transactionRepository.sumTransfersIn(user)) {
            movement.merge((Long) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        List<AccountBalance> list = accountRepository.findByUserOrderByActiveDescNameAsc(user).stream()
                .map(a -> new AccountBalance(a, a.getOpeningBalance().add(movement.getOrDefault(a.getId(), BigDecimal.ZERO))))
                .toList();
        List<AccountBalance> active = list.stream().filter(b -> b.account().isActive()).toList();
        BigDecimal total = Money.sum(active, AccountBalance::balance);
        BigDecimal liquid = Money.sum(active.stream().filter(b -> b.account().getType().isLiquid()).toList(), AccountBalance::balance);
        BigDecimal owed = Money.sum(active.stream()
                .filter(b -> b.account().getType() == AccountType.CREDIT_CARD && b.balance().signum() < 0).toList(),
                b -> b.balance().negate());
        return new Balances(list, total, liquid, owed);
    }

    @Transactional
    public Account save(AppUser user, AccountForm form) {
        Account a = form.isNew() ? new Account() : get(user, form.getId());
        a.setUser(user);
        a.setName(form.getName().trim());
        a.setType(form.getType());
        a.setInstitution(form.getInstitution() == null || form.getInstitution().isBlank() ? null : form.getInstitution().trim());
        a.setOpeningBalance(form.getOpeningBalance());
        a.setActive(form.isActive());
        return accountRepository.save(a);
    }

    /** Accounts with history can only be archived, so past months still add up. */
    @Transactional
    public void delete(AppUser user, Long id) {
        Account a = get(user, id);
        if (transactionRepository.usesAccount(a) || recurringRepository.existsByAccount(a)) {
            throw new BusinessException("\"" + a.getName() + "\" has transactions or recurring items. Archive it instead.");
        }
        accountRepository.delete(a);
    }

    @Transactional
    public Account toggleActive(AppUser user, Long id) {
        Account a = get(user, id);
        a.setActive(!a.isActive());
        return a;
    }
}
