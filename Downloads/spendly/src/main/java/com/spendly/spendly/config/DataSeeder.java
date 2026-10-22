package com.spendly.spendly.config;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.repository.*;
import com.spendly.spendly.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates the demo account (demo / demo123) with six months of realistic money activity, so every screen has
 * something to show on first start. Runs once: it does nothing if the demo user already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo123";

    private final AppUserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringItemRepository recurringRepository;
    private final SavingsGoalRepository goalRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${spendly.seed-demo-data:true}")
    private boolean seed;

    private final Random random = new Random(7);
    private AppUser user;
    private LocalDate today;
    private Map<String, Category> categories;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seed || userRepository.existsByUsernameIgnoreCase(DEMO_USERNAME)) {
            return;
        }
        seedDemo(LocalDate.now());
        log.info("Created demo account '{}' / '{}' with sample data", DEMO_USERNAME, DEMO_PASSWORD);
    }

    void seedDemo(LocalDate today) {
        this.today = today;
        user = new AppUser();
        user.setUsername(DEMO_USERNAME);
        user.setFullName("Karan Singh");
        user.setEmail("karan@example.com");
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user = userRepository.save(user);
        userService.createStarterKit(user);

        categories = categoryRepository.findByUserAndActiveTrueOrderByNameAsc(user).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));
        Map<String, Account> starter = accountRepository.findByUserAndActiveTrueOrderByNameAsc(user).stream()
                .collect(Collectors.toMap(Account::getName, Function.identity()));
        Account bank = starter.get("Bank account");
        bank.setName("HDFC Savings");
        bank.setInstitution("HDFC Bank");
        bank.setOpeningBalance(money(42_000));
        Account cash = starter.get("Cash");
        cash.setOpeningBalance(money(3_000));
        Account wallet = account("UPI Wallet", AccountType.WALLET, "Paytm", 1_500);
        Account card = account("Credit Card", AccountType.CREDIT_CARD, "ICICI Bank", 0);
        Account emergency = account("Emergency Fund", AccountType.SAVINGS, "SBI", 60_000);

        RecurringItem salary = recurring("Salary - Northwind Labs", TxnType.INCOME, bank, "Salary", 55_000, Frequency.MONTHLY, 1);
        RecurringItem rent = recurring("House rent", TxnType.EXPENSE, bank, "Rent & Housing", 15_000, Frequency.MONTHLY, 5);
        RecurringItem gym = recurring("Cult.fit membership", TxnType.EXPENSE, bank, "Health & Fitness", 1_500, Frequency.MONTHLY, 3);
        RecurringItem power = recurring("Electricity bill", TxnType.EXPENSE, bank, "Utilities & Bills", 1_500, Frequency.MONTHLY, 10);
        RecurringItem internet = recurring("Airtel broadband", TxnType.EXPENSE, bank, "Utilities & Bills", 799, Frequency.MONTHLY, 12);
        RecurringItem netflix = recurring("Netflix", TxnType.EXPENSE, card, "Subscriptions", 649, Frequency.MONTHLY, 15);
        RecurringItem spotify = recurring("Spotify", TxnType.EXPENSE, card, "Subscriptions", 119, Frequency.MONTHLY, 15);
        RecurringItem mobile = recurring("Jio mobile plan", TxnType.EXPENSE, wallet, "Utilities & Bills", 399, Frequency.MONTHLY, 18);
        RecurringItem insurance = recurring("Health insurance premium", TxnType.EXPENSE, bank, "Health & Fitness", 12_000, Frequency.YEARLY, 0);
        insurance.setNextDueDate(today.plusMonths(4).withDayOfMonth(15));

        YearMonth current = YearMonth.from(today);
        YearMonth first = current.minusMonths(5);
        BigDecimal cardSpendLastMonth = BigDecimal.ZERO;
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            int index = (int) java.time.temporal.ChronoUnit.MONTHS.between(first, m);
            boolean isCurrent = m.equals(current);
            BigDecimal cardSpend = BigDecimal.ZERO;

            post(salary, m, 55_000);
            post(gym, m, 1_500);
            post(rent, m, 15_000);
            post(power, m, 1_050 + random.nextInt(9) * 100);
            post(internet, m, 799);
            cardSpend = cardSpend.add(post(netflix, m, 649)).add(post(spotify, m, 119));
            post(mobile, m, 399);

            transfer(m, 1, bank, cash, 2_000, "ATM withdrawal");
            transfer(m, 2, bank, wallet, 4_000, "Wallet top-up");
            transfer(m, 16, bank, wallet, 4_000, "Wallet top-up");
            transfer(m, 7, bank, emergency, 5_000, "Monthly SIP to emergency fund");
            if (cardSpendLastMonth.signum() > 0) {
                transfer(m, 20, bank, card, cardSpendLastMonth.doubleValue(), "Credit card bill payment");
            }
            if (index % 2 == 1) {
                income(m, 18, bank, "Freelance", 3_500 + random.nextInt(12) * 500, pick("Logo design for Chai Point", "Website copy edits", "UI review for a startup"));
            }
            if (m.getMonthValue() % 3 == 0) {
                income(m, m.lengthOfMonth(), emergency, "Interest & Dividends", 540 + random.nextInt(120), "Savings interest");
            }

            int[] groceryDays = {4, 10, 16, 23, 28};
            for (int d : groceryDays) {
                expense(m, d, random.nextBoolean() ? wallet : bank, "Groceries", 650 + random.nextInt(24) * 50,
                        pick("BigBasket order", "Zepto groceries", "D-Mart weekly shop", "Vegetable market", "Blinkit order"));
            }
            for (int i = 0, n = 9 + random.nextInt(4); i < n; i++) {
                boolean onCard = random.nextInt(10) < 4;
                BigDecimal amt = expense(m, 1 + random.nextInt(m.lengthOfMonth()), onCard ? card : wallet, "Food & Dining",
                        180 + random.nextInt(16) * 50,
                        pick("Swiggy order", "Zomato order", "Lunch with team", "Coffee at Third Wave", "Dinner at Toit", "Pizza night", "Biryani from Meghana's", "Chai & samosa"));
                if (onCard) cardSpend = cardSpend.add(amt);
            }
            for (int i = 0, n = 10 + random.nextInt(5); i < n; i++) {
                boolean small = random.nextInt(3) > 0;
                expense(m, 1 + random.nextInt(m.lengthOfMonth()), small ? (random.nextBoolean() ? cash : wallet) : wallet, "Transport",
                        small ? 60 + random.nextInt(8) * 20 : 250 + random.nextInt(6) * 50,
                        small ? pick("Auto rickshaw", "Rapido bike", "Metro ride", "Bus pass top-up") : pick("Uber to airport", "Ola cab", "Petrol"));
            }
            int shops = isCurrent ? 0 : 2 + random.nextInt(2);
            for (int i = 0; i < shops; i++) {
                cardSpend = cardSpend.add(expense(m, 1 + random.nextInt(m.lengthOfMonth()), card, "Shopping", 600 + random.nextInt(18) * 100,
                        pick("Amazon order", "Myntra - kurta", "Decathlon running shoes", "IKEA home bits", "Nykaa order")));
            }
            for (int i = 0, n = 1 + random.nextInt(3); i < n; i++) {
                cardSpend = cardSpend.add(expense(m, 1 + random.nextInt(m.lengthOfMonth()), card, "Entertainment", 250 + random.nextInt(10) * 100,
                        pick("BookMyShow - movie", "Comedy show tickets", "Bowling with friends", "Steam game")));
            }
            expense(m, 1 + random.nextInt(m.lengthOfMonth()), wallet, "Personal care", 300 + random.nextInt(7) * 100, pick("Haircut", "Salon visit"));
            if (random.nextInt(3) == 0) {
                expense(m, 1 + random.nextInt(m.lengthOfMonth()), wallet, "Health & Fitness", 250 + random.nextInt(10) * 100, pick("Pharmacy - Apollo", "Doctor consultation"));
            }
            if (index == 2) {
                cardSpend = cardSpend.add(expense(m, 12, card, "Travel", 6_800, "Weekend trip to Coorg"));
                expense(m, 21, wallet, "Gifts & Donations", 1_500, "Birthday gift for Aditi");
            }
            if (index == 4) {
                expense(m, 9, bank, "Education", 499, "Udemy - Spring Boot course");
            }
            if (isCurrent) {
                // This month: shopping runs over budget so the dashboard has something to flag.
                cardSpend = cardSpend.add(expense(m, 3, card, "Shopping", 2_400, "Myntra festive sale"));
                cardSpend = cardSpend.add(expense(m, 8, card, "Shopping", 1_900, "Amazon - kitchen mixer"));
                cardSpend = cardSpend.add(expense(m, 14, card, "Shopping", 2_200, "Diwali shopping - FabIndia"));
                expense(m, 11, wallet, "Food & Dining", 1_150, "Team dinner at Truffles");
            }
            cardSpendLastMonth = cardSpend;

            budget(m, "Rent & Housing", 15_000);
            budget(m, "Groceries", 6_000);
            budget(m, "Food & Dining", 5_000);
            budget(m, "Transport", 3_500);
            budget(m, "Utilities & Bills", 3_000);
            budget(m, "Shopping", 6_000);
            budget(m, "Entertainment", 2_500);
            budget(m, "Health & Fitness", 2_500);
            budget(m, "Subscriptions", 1_000);
            budget(m, "Personal care", 1_000);
        }

        goal("Emergency fund", 3_00_000, 1_05_000, null, "#4f7a74", "Six months of spending, kept in the SBI savings account.");
        goal("Goa trip with friends", 40_000, 14_500, today.plusMonths(4).withDayOfMonth(1), "#b5562a", "Flights, stay and a little extra.");
        goal("New laptop", 85_000, 22_000, today.plusMonths(8).withDayOfMonth(1), "#3f5f80", null);
    }

    private Account account(String name, AccountType type, String institution, double opening) {
        Account a = new Account();
        a.setUser(user);
        a.setName(name);
        a.setType(type);
        a.setInstitution(institution);
        a.setOpeningBalance(money(opening));
        return accountRepository.save(a);
    }

    private RecurringItem recurring(String name, TxnType type, Account account, String category, double amount, Frequency frequency, int day) {
        RecurringItem r = new RecurringItem();
        r.setUser(user);
        r.setName(name);
        r.setType(type);
        r.setAccount(account);
        r.setCategory(categories.get(category));
        r.setAmount(money(amount));
        r.setFrequency(frequency);
        if (day > 0) {
            LocalDate thisMonth = YearMonth.from(today).atDay(day);
            r.setNextDueDate(thisMonth.isAfter(today) ? thisMonth : thisMonth.plusMonths(1));
        } else {
            r.setNextDueDate(today.plusMonths(1));
        }
        return recurringRepository.save(r);
    }

    /** Posts the occurrence of a monthly recurring item in month {@code m}, if that day has already happened. */
    private BigDecimal post(RecurringItem r, YearMonth m, double amount) {
        LocalDate date = m.atDay(Math.min(r.getNextDueDate().getDayOfMonth(), m.lengthOfMonth()));
        if (date.isAfter(today)) return BigDecimal.ZERO;
        Transaction t = txn(r.getType(), r.getAccount(), r.getCategory(), date, amount, r.getName());
        t.setRecurringItemId(r.getId());
        transactionRepository.save(t);
        return t.getAmount();
    }

    private BigDecimal expense(YearMonth m, int day, Account account, String category, double amount, String description) {
        LocalDate date = m.atDay(Math.min(day, m.lengthOfMonth()));
        if (date.isAfter(today)) return BigDecimal.ZERO;
        return transactionRepository.save(txn(TxnType.EXPENSE, account, categories.get(category), date, amount, description)).getAmount();
    }

    private void income(YearMonth m, int day, Account account, String category, double amount, String description) {
        LocalDate date = m.atDay(Math.min(day, m.lengthOfMonth()));
        if (!date.isAfter(today)) {
            transactionRepository.save(txn(TxnType.INCOME, account, categories.get(category), date, amount, description));
        }
    }

    private void transfer(YearMonth m, int day, Account from, Account to, double amount, String description) {
        LocalDate date = m.atDay(Math.min(day, m.lengthOfMonth()));
        if (date.isAfter(today)) return;
        Transaction t = txn(TxnType.TRANSFER, from, null, date, amount, description);
        t.setToAccount(to);
        transactionRepository.save(t);
    }

    private Transaction txn(TxnType type, Account account, Category category, LocalDate date, double amount, String description) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setType(type);
        t.setAccount(account);
        t.setCategory(category);
        t.setDate(date);
        t.setAmount(money(amount));
        t.setDescription(description);
        return t;
    }

    private void budget(YearMonth m, String category, double amount) {
        Budget b = new Budget();
        b.setUser(user);
        b.setCategory(categories.get(category));
        b.setPeriod(m.atDay(1));
        b.setAmount(money(amount));
        budgetRepository.save(b);
    }

    private void goal(String name, double target, double saved, LocalDate date, String color, String notes) {
        SavingsGoal g = new SavingsGoal();
        g.setUser(user);
        g.setName(name);
        g.setTargetAmount(money(target));
        g.setSavedAmount(money(saved));
        g.setTargetDate(date);
        g.setColor(color);
        g.setNotes(notes);
        goalRepository.save(g);
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
