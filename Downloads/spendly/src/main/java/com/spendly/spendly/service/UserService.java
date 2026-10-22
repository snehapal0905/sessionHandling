package com.spendly.spendly.service;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.PasswordChangeForm;
import com.spendly.spendly.dto.ProfileForm;
import com.spendly.spendly.dto.RegisterForm;
import com.spendly.spendly.repository.AccountRepository;
import com.spendly.spendly.repository.AppUserRepository;
import com.spendly.spendly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    /** name, colour, essential (needs vs wants) */
    private static final Object[][] EXPENSE_CATEGORIES = {
            {"Rent & Housing", "#5c2a10", true},
            {"Groceries", "#6f8f6a", true},
            {"Food & Dining", "#b5562a", false},
            {"Transport", "#3f5f80", true},
            {"Utilities & Bills", "#d9922b", true},
            {"Shopping", "#c97b63", false},
            {"Entertainment", "#7d5a8c", false},
            {"Health & Fitness", "#4f7a74", true},
            {"Subscriptions", "#8fb3d9", false},
            {"Education", "#a3a86b", true},
            {"Personal care", "#d4a373", false},
            {"Travel", "#9c6b4e", false},
            {"Gifts & Donations", "#f2b48a", false},
            {"Other", "#b8a99a", false},
    };
    private static final Object[][] INCOME_CATEGORIES = {
            {"Salary", "#6f8f6a"},
            {"Freelance", "#3f5f80"},
            {"Interest & Dividends", "#d9922b"},
            {"Gifts received", "#c97b63"},
            {"Other income", "#b8a99a"},
    };

    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    /** Creates the user with a starter set of categories and two accounts, so they can record money straight away. */
    @Transactional
    public AppUser register(RegisterForm form) {
        if (userRepository.existsByUsernameIgnoreCase(form.getUsername().trim())) {
            throw new BusinessException("That username is taken, please choose another");
        }
        AppUser user = new AppUser();
        user.setUsername(form.getUsername().trim());
        user.setFullName(form.getFullName().trim());
        user.setEmail(blankToNull(form.getEmail()));
        user.setCurrencySymbol(form.getCurrencySymbol().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user = userRepository.save(user);
        createStarterKit(user);
        return user;
    }

    public void createStarterKit(AppUser user) {
        for (Object[] c : EXPENSE_CATEGORIES) {
            categoryRepository.save(category(user, CategoryKind.EXPENSE, (String) c[0], (String) c[1], (Boolean) c[2]));
        }
        for (Object[] c : INCOME_CATEGORIES) {
            categoryRepository.save(category(user, CategoryKind.INCOME, (String) c[0], (String) c[1], false));
        }
        accountRepository.save(account(user, "Bank account", AccountType.BANK));
        accountRepository.save(account(user, "Cash", AccountType.CASH));
    }

    @Transactional
    public void updateProfile(ProfileForm form) {
        AppUser user = userRepository.findById(currentUser.get().getId()).orElseThrow();
        user.setFullName(form.getFullName().trim());
        user.setEmail(blankToNull(form.getEmail()));
        user.setCurrencySymbol(form.getCurrencySymbol().trim());
        user.setSavingsTargetPercent(form.getSavingsTargetPercent());
    }

    @Transactional
    public void changeOwnPassword(PasswordChangeForm form) {
        AppUser user = userRepository.findById(currentUser.get().getId()).orElseThrow();
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Your current password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
    }

    @Transactional
    public void recordLogin(String username) {
        userRepository.findByUsernameIgnoreCase(username).ifPresent(u -> u.setLastLoginAt(LocalDateTime.now()));
    }

    private static Category category(AppUser user, CategoryKind kind, String name, String color, boolean essential) {
        Category c = new Category();
        c.setUser(user);
        c.setKind(kind);
        c.setName(name);
        c.setColor(color);
        c.setEssential(essential);
        return c;
    }

    private static Account account(AppUser user, String name, AccountType type) {
        Account a = new Account();
        a.setUser(user);
        a.setName(name);
        a.setType(type);
        a.setOpeningBalance(BigDecimal.ZERO);
        return a;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
