# Spendly

A personal budget planner: plan budgets and compare income vs. expenses.
Java 21 · Spring Boot 4.1 · Spring Security · JPA (MySQL or H2) · Thymeleaf.
The UI follows the Stockly design system (Kalnia / Onest / Playwrite type, arch shapes, pill buttons) with a washed, muted orange palette.

## Run it

```bash
# no database needed - H2 file database in ./data
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# or with MySQL (defaults: localhost:3306, root/root - override with DB_URL, DB_USERNAME, DB_PASSWORD)
./mvnw spring-boot:run
```

Open http://localhost:8080 (set `PORT` to change it). Sign in as **demo / demo123** for six months of sample data,
or create your own account at `/register`. Turn the demo account off with `spendly.seed-demo-data=false`.

## What's inside

| Area | Pages |
| --- | --- |
| Overview | Dashboard (balance, income vs expenses, budget utilization, where it went, safe to spend), Health score |
| Money | Transactions (filters, CSV export), Accounts (bank, cash, wallet, card, savings, investment) |
| Planning | Budgets (per category per month, copy last month, suggest from spending), Bills & recurring, Savings goals |
| Intelligence | Insights (month-end forecast, overspending risks, tips), Reports (6/12-month trend, category breakdown, needs vs wants) |
| Organise | Categories (colours, needs vs wants), My account (currency, savings target, password) |

Rules worth knowing:

- Transfers (including paying a credit-card bill) move money between your accounts; they are never income or spending.
- The forecast adds bills and income still due (from recurring items) to your day-to-day spending pace, blended with the last three months early in the month.
- Safe to spend = this month's income − spent − bills still due − savings target.
- Every query is scoped to the signed-in user; forms use DTOs so ids from another account are rejected (404).

## Tests

```bash
./mvnw test
```

Renders every page as the demo user, walks the main write flows and checks one user cannot read or delete another's records.

Landing-page photos: Wikimedia Commons, credited in the page footer.
