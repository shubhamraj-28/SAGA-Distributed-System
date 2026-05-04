# SAGA Distributed System

A implementation of the **Saga Pattern** for coordinating distributed transactions across microservices. This system demonstrates how to reliably execute multi-step business workflows while maintaining data consistency across independently deployed services using **Spring Boot**, **Apache Kafka**, and **PostgreSQL**.

## 🎯 Overview

The SAGA Distributed System coordinates an **order fulfillment workflow** by orchestrating three independent microservices. Each service manages its own database and communicates via **event-driven architecture** using Kafka. If any step fails, automatic **compensating transactions** roll back previous changes—ensuring eventual consistency even in failure scenarios.

### Key Features
- ✅ **Saga Orchestration Pattern** - Centralized workflow engine with clear step dependencies
- ✅ **Event-Driven Communication** - Asynchronous Kafka-based message passing
- ✅ **Distributed Transactions** - Multi-step workflow with automatic rollback on failure
- ✅ **Compensating Transactions** - Undo operations for inventory reserves and payment charges
- ✅ **Audit Logging** - Complete step-by-step transaction history and status tracking
- ✅ **Automated Testing** - Integration tests with Docker, Kafka, and PostgreSQL
- ✅ **Schema-Per-Service** - Independent database schemas for data isolation

## 📋 Architecture Overview

| Service | Port | Responsibility |
|---------|------|-----------------|
| **saga-orchestrator** | 8084 | REST API, saga engine, workflow orchestration |
| **inventory-service** | 8081 | Inventory management, stock reservations |
| **payment-service** | 8082 | Payment processing, charge & refund operations |
| **notification-service** | 8083 | User notifications via Kafka |

### Order Saga Workflow

The system executes a three-step saga:

| Step | Service | Command Topic | Reply Topic | Compensation Topic |
|------|---------|---------------|------------|-------------------|
| 1️⃣ | Inventory | `inventory.commands` | `inventory.replies` | `inventory.compensate` |
| 2️⃣ | Payment | `payment.commands` | `payment.replies` | `payment.compensate` |
| 3️⃣ | Notification | `notification.commands` | `notification.replies` | _(no rollback needed)_ |

**Success Path:** Reserve Inventory → Charge Payment → Send Notification  
**Failure Path:** If any step fails, prior steps are automatically compensated (reversed)

### Infrastructure Components

- **PostgreSQL 17** - Schema-per-service pattern (port 5434)
  - `inventory` schema - Stock reservations and inventory management
  - `payment` schema - Transaction records and payment history
  - `orchestrator` schema - Saga state management & audit logs
- **Apache Kafka** (KRaft mode) - Event streaming and message broker (port 9092)
- **UTC Timezone** - All database connections normalized to UTC for cross-timezone consistency

## Prerequisites

- **JDK 25** or later
- **Maven 3.9+**
- **Docker Desktop** (or Docker Engine)
- **Git**

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
