package com.spendly.spendly.repository;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Budget;
import com.spendly.spendly.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndPeriod(AppUser user, LocalDate period);

    List<Budget> findByUserAndPeriodBetween(AppUser user, LocalDate from, LocalDate to);

    boolean existsByCategory(Category category);

    void deleteByCategory(Category category);
}
