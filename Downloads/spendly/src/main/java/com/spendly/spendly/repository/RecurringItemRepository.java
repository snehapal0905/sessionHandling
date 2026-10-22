package com.spendly.spendly.repository;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.RecurringItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringItemRepository extends JpaRepository<RecurringItem, Long> {

    List<RecurringItem> findByUserOrderByActiveDescNextDueDateAsc(AppUser user);

    List<RecurringItem> findByUserAndActiveTrue(AppUser user);

    Optional<RecurringItem> findByIdAndUser(Long id, AppUser user);

    boolean existsByAccount(Account account);

    boolean existsByCategory(Category category);
}
