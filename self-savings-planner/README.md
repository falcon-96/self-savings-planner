# Self Savings Planner

Automated retirement savings through expense-based micro-investments. Every purchase is rounded up to the nearest ₹100 —
the difference (remnant) is invested via NPS or Index funds with projected returns adjusted for inflation and tax
benefits.

**BlackRock Hackathon 2026** · Spring Boot 4 · Java 21 · Docker

---

## Quick Start

### Local

```bash
./mvnw spring-boot:run
# App runs on http://localhost:5477
# Swagger UI: http://localhost:5477/swagger-ui/index.html
```

### Docker

```bash
./mvnw clean package -DskipTests
docker build -t blk-hacking-ind-chetan-dhanjal .
docker run -d -p 5477:5477 blk-hacking-ind-chetan-dhanjal
```

### Docker Compose

```bash
docker compose up -d
```

---

## API Endpoints

| Method | Endpoint                                         | Description                                        |
|--------|--------------------------------------------------|----------------------------------------------------|
| `POST` | `/blackrock/challenge/v1/transactions/parse`     | Round up transactions → ceiling + remnant          |
| `POST` | `/blackrock/challenge/v1/transactions/validator` | Validate against wage, detect duplicates/negatives |
| `POST` | `/blackrock/challenge/v1/transactions/filter`    | Validate + mark K-period membership                |
| `POST` | `/blackrock/challenge/v1/returns/nps`            | Project NPS returns (7.11%) + tax benefit          |
| `POST` | `/blackrock/challenge/v1/returns/index`          | Project Index fund returns (14.49%)                |
| `GET`  | `/blackrock/challenge/v1/performance`            | JVM uptime, memory %, active threads               |

Full interactive documentation available at `/swagger-ui/index.html`.

---

## How It Works

```
Expense ₹375  →  Ceiling ₹400  →  Remnant ₹25 (auto-saved)
```

### Period Rules

| Period | Effect                                | Resolution                  |
|--------|---------------------------------------|-----------------------------|
| **Q**  | Overrides remnant with a fixed amount | Latest start date wins      |
| **P**  | Adds extra to remnant                 | All matching extras stack   |
| **K**  | Groups transactions for investment    | Savings summed per K period |

### Returns Calculation

1. Compute ceiling/remnant per transaction
2. Apply Q rules → P rules (in sequence)
3. Group by K periods, sum remnants
4. Compound interest: `A = P × (1 + rate)^years`
5. Inflation adjustment: `A_real = A / (1 + inflation)^years`
6. Tax benefit (NPS only): Section 80CCD, `min(invested, 10% income, ₹2L)`

| Strategy         | Annual Rate |
|------------------|-------------|
| NPS              | 7.11%       |
| Index (NIFTY 50) | 14.49%      |

---

## Example

**Request** (`POST /returns/nps`):

```json
{
  "age": 29,
  "wage": 50000,
  "inflation": 5.5,
  "q": [
    {
      "fixed": 0,
      "start": "2023-07-01 00:00:00",
      "end": "2023-07-31 23:59:59"
    }
  ],
  "p": [
    {
      "extra": 25,
      "start": "2023-10-01 08:00:00",
      "end": "2023-12-31 19:59:59"
    }
  ],
  "k": [
    {
      "start": "2023-01-01 00:00:00",
      "end": "2023-12-31 23:59:59"
    }
  ],
  "transactions": [
    {
      "date": "2023-02-28 15:49:20",
      "amount": 375
    },
    {
      "date": "2023-07-01 21:59:00",
      "amount": 620
    },
    {
      "date": "2023-10-12 20:15:30",
      "amount": 250
    },
    {
      "date": "2023-12-17 08:09:45",
      "amount": 480
    }
  ]
}
```

**Response**:

```json
{
  "totalTransactionAmount": 1725.0,
  "totalCeiling": 1900.0,
  "savingsByDates": [
    {
      "start": "2023-01-01 00:00:00",
      "end": "2023-12-31 23:59:59",
      "amount": 145.0,
      "profit": 86.88,
      "taxBenefit": 0.0
    }
  ]
}
```

---

## Project Structure

```
src/main/java/.../
├── config/
│   └── OpenApiConfig.java
├── controller/
│   ├── TransactionController.java
│   ├── ReturnsController.java
│   └── PerformanceController.java
├── dto/
│   ├── common/   TemporalData
│   ├── period/   Q, P, K
│   ├── request/  TransactionInput, ValidatorRequest, FilterRequest, ReturnsRequest
│   └── response/ EnrichedTransaction, ValidationResult, ReturnsResponse, Saving, ...
└── service/
    ├── TransactionService.java
    └── ReturnsService.java
```

---

## Testing

```bash
./mvnw test
```

## Tech Stack

- **Java 21** · **Spring Boot 4.0.3**
- **SpringDoc OpenAPI** (Swagger UI)
- **Docker** (Alpine + Eclipse Temurin JRE 21)
- **Maven** (build)
