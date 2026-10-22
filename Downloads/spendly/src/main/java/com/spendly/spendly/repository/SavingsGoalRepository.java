package com.spendly.spendly.repository;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserOrderByTargetDateAscNameAsc(AppUser user);

    Optional<SavingsGoal> findByIdAndUser(Long id, AppUser user);
}
