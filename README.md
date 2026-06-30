# Loan Engine

Spring Boot and MySQL API for a core banking loan prepayment engine.

## Assessment Scope

This project implements only:

- Category A - Option A.
- Partial prepayment of principal.
- EMI is reduced after prepayment.
- Remaining tenor is kept fixed.

This project intentionally does not implement:

- Category B early settlement.
- Category A Option B or Option C.
- Authentication.
- Frontend.
- Admin dashboard.
- Unrelated banking workflows.

## Business Assumptions

The supplied CSV file is treated as the exact schedule reference:

`docs/assessment/Prepayments & Early Settlement Loan - Loan.csv`

The assessment PDF mentions an outstanding principal of approximately `680,000` after 24 months, but the CSV schedule gives the exact closing balance after installment 24 as `669,724.82`. The implementation and tests use the CSV value.

Base loan reference:

- Principal: `1,000,000.00`
- Annual interest rate: `12%`
- Interest basis: reducing balance
- Tenor: `60` months
- Monthly EMI: `22,244.45`
- Installment 1 interest: `10,000.00`
- Installment 1 principal: `12,244.45`
- Installment 1 closing balance: `987,755.55`
- Installment 24 interest: `6,851.18`
- Installment 24 principal: `15,393.27`
- Installment 24 closing balance: `669,724.82`
- Total interest: about `334,666.86`

Prepayment reference:

- Prepayment amount: `200,000.00`
- Applied after installment: `24`
- Balance before prepayment: `669,724.82`
- Balance after prepayment: `469,724.82`
- Remaining months: `36`
- Old EMI: `22,244.45`
- New EMI: `15,601.59`

## afterInstallmentNumber Semantics

The prepayment request uses `afterInstallmentNumber`.

`afterInstallmentNumber = 24` means installments 1 through 24 have already been paid, and the prepayment is applied immediately after installment 24.

Remaining months are calculated as:

```text
tenorMonths - afterInstallmentNumber
```

For the base loan:

```text
60 - 24 = 36
```

The implementation is not hardcoded to installment 24. Installment 24 appears only in test and documentation examples.

## Tech Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Jakarta Validation
- Flyway
- MySQL 8.4
- Testcontainers with MySQL 8.4
- Maven wrapper

## Architecture

The code is organized into clean layers:

- `controller`
- `dto`
- `entity`
- `repository`
- `service`
- `strategy`
- `exception`

Financial values use `BigDecimal`. Money values are rounded to scale 2 with `RoundingMode.HALF_UP`.

Write operations are transactional:

- Loan creation.
- Partial prepayment.

Prepayment calculation uses a strategy pattern. The only supported strategy is:

```text
REDUCE_EMI_KEEP_TENOR
```

## Database

Flyway creates and validates these tables:

- `loans`
- `loan_schedules`
- `loan_transactions`

The application uses:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/loan_engine?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=loan_user
spring.datasource.password=loan_pass
spring.jpa.hibernate.ddl-auto=validate
```

## Run MySQL Locally

Start MySQL:

```bash
docker compose up -d mysql
```

Confirm it is running:

```bash
docker compose ps
```

The Docker Compose file starts MySQL 8.4 with:

- Database: `loan_engine`
- User: `loan_user`
- Password: `loan_pass`
- Port: `3306`

## Run The App

Start the API:

```bash
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

Flyway runs automatically on startup.

## Run Tests

Run the full test suite:

```bash
./mvnw clean test
```

Integration tests use Testcontainers with MySQL 8.4.

Run only loan API integration tests:

```bash
./mvnw -Dtest=LoanApiIntegrationTest test
```

Run only prepayment API integration tests:

```bash
./mvnw -Dtest=PrepaymentApiIntegrationTest test
```

## API Endpoints

### Create Loan

`POST /api/loans`

Request:

```json
{
  "principalAmount": 1000000,
  "annualInterestRate": 12,
  "tenorMonths": 60,
  "startDate": "2024-07-24"
}
```

Example:

```bash
curl -s -X POST http://localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{
    "principalAmount": 1000000,
    "annualInterestRate": 12,
    "tenorMonths": 60,
    "startDate": "2024-07-24"
  }'
```

Expected response shape:

```json
{
  "loanId": 1,
  "principalAmount": 1000000.00,
  "annualInterestRate": 12,
  "tenorMonths": 60,
  "monthlyEmi": 22244.45,
  "status": "ACTIVE",
  "startDate": "2024-07-24"
}
```

### Get Loan

`GET /api/loans/{loanId}`

Example:

```bash
curl -s http://localhost:8080/api/loans/1
```

### Get Schedule

`GET /api/loans/{loanId}/schedule`

Example:

```bash
curl -s http://localhost:8080/api/loans/1/schedule
```

The schedule is ordered by installment number. The base loan schedule has exactly 60 rows.

Reference rows:

```json
{
  "installmentNumber": 1,
  "dueDate": "2024-07-24",
  "openingBalance": 1000000.00,
  "emiAmount": 22244.45,
  "interestComponent": 10000.00,
  "principalComponent": 12244.45,
  "closingBalance": 987755.55,
  "status": "PENDING"
}
```

```json
{
  "installmentNumber": 24,
  "interestComponent": 6851.18,
  "principalComponent": 15393.27,
  "closingBalance": 669724.82,
  "status": "PENDING"
}
```

### Apply Partial Prepayment

`POST /api/loans/{loanId}/prepayments`

Request:

```json
{
  "afterInstallmentNumber": 24,
  "amount": 200000,
  "strategy": "REDUCE_EMI_KEEP_TENOR"
}
```

Example:

```bash
curl -s -X POST http://localhost:8080/api/loans/1/prepayments \
  -H 'Content-Type: application/json' \
  -d '{
    "afterInstallmentNumber": 24,
    "amount": 200000,
    "strategy": "REDUCE_EMI_KEEP_TENOR"
  }'
```

Expected response:

```json
{
  "loanId": 1,
  "strategy": "REDUCE_EMI_KEEP_TENOR",
  "afterInstallmentNumber": 24,
  "balanceBefore": 669724.82,
  "prepaymentAmount": 200000.00,
  "balanceAfter": 469724.82,
  "remainingMonths": 36,
  "oldEmi": 22244.45,
  "newEmi": 15601.59
}
```

After a successful prepayment:

- Installments 1 through `afterInstallmentNumber` are marked `PAID`.
- Future installments are recalculated and marked `ADJUSTED`.
- A `PREPAYMENT` row is written to `loan_transactions`.
- The loan's current `monthlyEmi` is updated to the reduced EMI.

## Error Responses

Invalid requests return structured JSON:

```json
{
  "timestamp": "2026-06-30T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/loans",
  "fieldErrors": {
    "principalAmount": "must be greater than 0.00"
  }
}
```

Handled cases include:

- Loan not found.
- Invalid request body.
- Validation failures.
- Invalid installment number.
- Unsupported strategy.
- Negative or zero prepayment amount.
- Prepayment amount greater than or equal to outstanding balance.
- Prepayment on a non-active loan.

## Useful Manual Flow

Start MySQL:

```bash
docker compose up -d mysql
```

Start the app:

```bash
./mvnw spring-boot:run
```

Create the base loan:

```bash
curl -s -X POST http://localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{
    "principalAmount": 1000000,
    "annualInterestRate": 12,
    "tenorMonths": 60,
    "startDate": "2024-07-24"
  }'
```

Apply the sample prepayment:

```bash
curl -s -X POST http://localhost:8080/api/loans/1/prepayments \
  -H 'Content-Type: application/json' \
  -d '{
    "afterInstallmentNumber": 24,
    "amount": 200000,
    "strategy": "REDUCE_EMI_KEEP_TENOR"
  }'
```

Fetch the adjusted schedule:

```bash
curl -s http://localhost:8080/api/loans/1/schedule
```
