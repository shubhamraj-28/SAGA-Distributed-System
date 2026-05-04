# SAGA Distributed System

Choreography-style **distributed saga** for coordinating a multi-step business transaction across separate Spring Boot microservices. An **orchestrator** drives the flow over **Apache Kafka**; each step service owns its data in **PostgreSQL** (separate schemas in one database). Failed steps trigger **compensating transactions** on prior steps where supported.

## Architecture

| Service | Port | Role |
|--------|------|------|
| **inventory-service** | 8081 | Reserve / release stock (`inventory` schema) |
| **payment-service** | 8082 | Charge / refund (`payment` schema) |
| **notification-service** | 8083 | Send notification (Kafka only; no DB) |
| **saga-orchestrator** | 8084 | REST API + saga engine + persistence (`orchestrator` schema) |

**ORDER_SAGA** steps (see `SagaDefinitionRegistry`):

1. `RESERVE_INVENTORY` → topics `inventory.commands` / `inventory.replies` / `inventory.compensate`
2. `CHARGE_PAYMENT` → `payment.commands` / `payment.replies` / `payment.compensate`
3. `SEND_NOTIFICATION` → `notification.commands` / `notification.replies` (no compensation topic)

PostgreSQL runs on host port **5434** (container `5432`). Kafka broker is advertised at **localhost:9092** for apps running on your machine.

JDBC URLs set the session timezone to **UTC** (`options=-c TimeZone=UTC`) so clients whose JVM uses legacy zone IDs (for example `Asia/Calcutta`) still connect cleanly to PostgreSQL 17.

## Prerequisites

- **JDK 25** and **Maven 3.9+**
- **Docker Desktop** (or Docker Engine) for infrastructure and for automated tests

## Run infrastructure (Docker Compose)

From the repository root:

```powershell
docker compose up -d
```

This starts:

- **PostgreSQL 17** with `init-db/01-create-schemas.sql` (schemas + seed products)
- **Kafka** (KRaft, single broker)

Wait until Postgres is healthy (Compose defines a healthcheck), then start the four Spring Boot applications (four terminals or your IDE).

### Start microservices (local JVM)

Use each module’s Maven wrapper or `mvn spring-boot:run` from the nested project directory, in any order (all need Kafka + Postgres up first):

```powershell
cd "inventory-service\inventory-service"; .\mvnw spring-boot:run
cd "payment-service\payment-service"; .\mvnw spring-boot:run
cd "notification-service\notification-service"; .\mvnw spring-boot:run
cd "saga-orchestrator\saga-orchestrator"; .\mvnw spring-boot:run
```

### Trigger a saga (REST)

```powershell
$body = @{
  userId = "user-42"
  email  = "user@example.com"
  items  = @(@{ sku = "SKU-MOUSE-001"; quantity = 1; price = 29.99 })
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8084/api/sagas" -Body $body -ContentType "application/json"
```

Poll status until `COMPLETED` (or `FAILED` if a step fails):

```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/sagas/<SAGA_ID>"
```

Step audit log:

```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/sagas/<SAGA_ID>/logs"
```

### Demo: forced notification failure (compensation)

By default, notifications **succeed** so the happy path completes. To simulate a failure at the last step and exercise inventory release + payment refund, run the notification service with the **`demo`** profile:

```powershell
cd "notification-service\notification-service"
.\mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

(`application-demo.yml` sets `notification.simulate-failure: true`.)

## Automated tests

Start the same infrastructure Compose provides, then run Maven (tests talk to **localhost:5434** and **localhost:9092**):

```powershell
docker compose up -d
mvn test
```

Or a single module:

```powershell
cd "saga-orchestrator\saga-orchestrator"; .\mvnw test
```

What is covered:

- **saga-orchestrator**: full `ORDER_SAGA` to `COMPLETED` against Postgres + Kafka from Compose, with in-test stub participants replying on command topics (worker apps do not need to run).
- **inventory-service** / **payment-service**: publish a command to Kafka; listener updates PostgreSQL; assertions on stock or row count.
- **notification-service**: **Embedded Kafka** (no Docker required for this module), success path with `simulate-failure=false`.

## Configuration reference

| Setting | Purpose |
|--------|---------|
| `notification.simulate-failure` | `true` forces the notification step to throw (demo compensation). Default in `application.yml` is `false`. |
| `saga.timeout.*` | Orchestrator timeout scheduler (see `application.yml`). |

## Repository layout

```
docker-compose.yml          # Postgres + Kafka
init-db/                    # Postgres init SQL
saga-orchestrator/          # Orchestrator Spring Boot app
inventory-service/
payment-service/
notification-service/
```

Remote: [SAGA-Distributed-System on GitHub](https://github.com/shubhamraj-28/SAGA-Distributed-System)
