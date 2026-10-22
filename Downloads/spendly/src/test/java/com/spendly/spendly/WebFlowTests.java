package com.spendly.spendly;

import com.spendly.spendly.domain.*;
import com.spendly.spendly.dto.RegisterForm;
import com.spendly.spendly.repository.*;
import com.spendly.spendly.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renders every page as the demo user and walks through the main write flows. */
@SpringBootTest
@AutoConfigureMockMvc
class WebFlowTests {

    private static final RequestPostProcessor DEMO = user("demo");

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired RecurringItemRepository recurringRepository;
    @Autowired SavingsGoalRepository goalRepository;
    @Autowired BudgetRepository budgetRepository;
    @Autowired UserService userService;

    AppUser demo;

    @BeforeEach
    void load() {
        demo = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
    }

    @Test
    void publicPagesRenderForVisitors() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("spend<b>ly</b>")));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void everyAppPageRenders() throws Exception {
        Transaction t = transactionRepository.findTop8ByUserOrderByDateDescIdDesc(demo).getFirst();
        Account a = accountRepository.findByUserAndActiveTrueOrderByNameAsc(demo).getFirst();
        Category c = categoryRepository.findByUserAndActiveTrueOrderByNameAsc(demo).getFirst();
        RecurringItem r = recurringRepository.findByUserAndActiveTrue(demo).getFirst();
        SavingsGoal g = goalRepository.findByUserOrderByTargetDateAscNameAsc(demo).getFirst();
        String previous = YearMonth.now().minusMonths(1).toString();
        List<String> pages = List.of("/dashboard", "/health", "/insights", "/account",
                "/transactions", "/transactions?type=EXPENSE&q=swiggy", "/transactions?month=" + previous,
                "/transactions/new", "/transactions/new?type=INCOME", "/transactions/new?type=TRANSFER", "/transactions/" + t.getId() + "/edit",
                "/accounts", "/accounts/new", "/accounts/" + a.getId() + "/edit",
                "/categories", "/categories/new", "/categories/" + c.getId() + "/edit",
                "/budgets", "/budgets?month=" + previous, "/budgets?month=" + YearMonth.now().plusMonths(1),
                "/recurring", "/recurring/new", "/recurring/" + r.getId() + "/edit",
                "/goals", "/goals/new", "/goals/" + g.getId() + "/edit",
                "/reports", "/reports?range=12&month=" + previous);
        for (String page : pages) {
            mvc.perform(get(page).with(DEMO)).andExpect(status().isOk());
        }
        mvc.perform(get("/transactions/export").with(DEMO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void recordingAnExpenseUpdatesTheLedger() throws Exception {
        Account wallet = accountRepository.findByUserAndActiveTrueOrderByNameAsc(demo).stream()
                .filter(a -> a.getType() == AccountType.WALLET).findFirst().orElseThrow();
        Category food = categoryRepository.findByUserAndActiveTrueOrderByNameAsc(demo).stream()
                .filter(x -> x.getName().equals("Food & Dining")).findFirst().orElseThrow();
        long before = transactionRepository.countByUser(demo);

        mvc.perform(post("/transactions/save").with(DEMO).with(csrf())
                        .param("type", "EXPENSE").param("accountId", wallet.getId().toString())
                        .param("categoryId", food.getId().toString()).param("amount", "420.50")
                        .param("date", LocalDate.now().toString()).param("description", "Test thali"))
                .andExpect(status().is3xxRedirection());
        assertThat(transactionRepository.countByUser(demo)).isEqualTo(before + 1);

        // a transfer without a destination is rejected by validation and re-shows the form
        mvc.perform(post("/transactions/save").with(DEMO).with(csrf())
                        .param("type", "TRANSFER").param("accountId", wallet.getId().toString())
                        .param("amount", "100").param("date", LocalDate.now().toString()).param("description", "Oops"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pick a different account")));
    }

    @Test
    void budgetsCanBeSavedAndSuggested() throws Exception {
        Category travel = categoryRepository.findByUserAndActiveTrueOrderByNameAsc(demo).stream()
                .filter(x -> x.getName().equals("Travel")).findFirst().orElseThrow();
        YearMonth next = YearMonth.now().plusMonths(2);
        mvc.perform(post("/budgets/save").with(DEMO).with(csrf())
                        .param("month", next.toString()).param("budget_" + travel.getId(), "8000"))
                .andExpect(status().is3xxRedirection());
        assertThat(budgetRepository.findByUserAndPeriod(demo, next.atDay(1))).hasSize(1);

        mvc.perform(post("/budgets/suggest").with(DEMO).with(csrf()).param("month", next.plusMonths(1).toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void postingARecurringBillRecordsItAndMovesTheDueDate() throws Exception {
        RecurringItem netflix = recurringRepository.findByUserAndActiveTrue(demo).stream()
                .filter(r -> r.getName().equals("Netflix")).findFirst().orElseThrow();
        LocalDate due = netflix.getNextDueDate();
        mvc.perform(post("/recurring/" + netflix.getId() + "/post").with(DEMO).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(recurringRepository.findById(netflix.getId()).orElseThrow().getNextDueDate()).isEqualTo(due.plusMonths(1));
    }

    @Test
    void goalsTakeContributions() throws Exception {
        SavingsGoal goal = goalRepository.findByUserOrderByTargetDateAscNameAsc(demo).getFirst();
        mvc.perform(post("/goals/" + goal.getId() + "/contribute").with(DEMO).with(csrf()).param("amount", "1000").param("action", "add"))
                .andExpect(status().is3xxRedirection());
        assertThat(goalRepository.findById(goal.getId()).orElseThrow().getSavedAmount())
                .isEqualByComparingTo(goal.getSavedAmount().add(java.math.BigDecimal.valueOf(1000)));
    }

    @Test
    void registrationCreatesAPrivateStarterKit() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Kabir Mehta").param("username", "kabir").param("email", "")
                        .param("currencySymbol", "₹").param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(status().is3xxRedirection());
        AppUser kabir = userRepository.findByUsernameIgnoreCase("kabir").orElseThrow();
        assertThat(categoryRepository.findByUserAndActiveTrueOrderByNameAsc(kabir)).isNotEmpty();
        assertThat(accountRepository.findByUserAndActiveTrueOrderByNameAsc(kabir)).hasSize(2);

        mvc.perform(get("/dashboard").with(user("kabir"))).andExpect(status().isOk());
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Someone").param("username", "KABIR").param("currencySymbol", "₹")
                        .param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("username is taken")));
    }

    @Test
    void usersCannotSeeEachOthersRecords() throws Exception {
        if (userRepository.findByUsernameIgnoreCase("intruder").isEmpty()) {
            RegisterForm f = new RegisterForm();
            f.setFullName("Intruder");
            f.setUsername("intruder");
            f.setCurrencySymbol("$");
            f.setPassword("secret1");
            f.setConfirmPassword("secret1");
            userService.register(f);
        }
        Transaction t = transactionRepository.findTop8ByUserOrderByDateDescIdDesc(demo).getFirst();
        mvc.perform(get("/transactions/" + t.getId() + "/edit").with(user("intruder"))).andExpect(status().isNotFound());
        mvc.perform(post("/transactions/" + t.getId() + "/delete").with(user("intruder")).with(csrf())).andExpect(status().isNotFound());
        assertThat(transactionRepository.findById(t.getId())).isPresent();
    }
}
