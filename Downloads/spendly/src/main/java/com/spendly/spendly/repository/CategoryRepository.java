package com.spendly.spendly.repository;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.CategoryKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserOrderByKindAscActiveDescNameAsc(AppUser user);

    List<Category> findByUserAndKindAndActiveTrueOrderByNameAsc(AppUser user, CategoryKind kind);

    List<Category> findByUserAndActiveTrueOrderByNameAsc(AppUser user);

    Optional<Category> findByIdAndUser(Long id, AppUser user);

    boolean existsByUserAndKindAndNameIgnoreCase(AppUser user, CategoryKind kind, String name);
}
