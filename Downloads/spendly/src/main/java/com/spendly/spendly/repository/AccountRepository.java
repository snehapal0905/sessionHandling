package com.spendly.spendly.repository;

import com.spendly.spendly.domain.Account;
import com.spendly.spendly.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserOrderByActiveDescNameAsc(AppUser user);

    List<Account> findByUserAndActiveTrueOrderByNameAsc(AppUser user);

    Optional<Account> findByIdAndUser(Long id, AppUser user);
}
