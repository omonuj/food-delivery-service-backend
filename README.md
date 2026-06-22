# 🍽️ Food Ordering System — Microservices Architecture

A Food Ordering System built with Microservices, Domain-Driven Design (DDD), Hexagonal Architecture, the SAGA and Outbox patterns, and Kafka for asynchronous, event-driven communication.

This project demonstrates how to design a scalable, fault-tolerant, event-driven distributed system similar to platforms like Uber Eats or DoorDash.

## 🚀 Services

The system is composed of independently deployable services that communicate via Kafka events:

| Service | Port | Responsibility |
|---|---|---|
| **Order Service** | 8181 | Order creation, order tracking, and SAGA orchestration |
| **Payment Service** | 8182 | Credit validation and payment processing |
| **Restaurant Service** | 8183 | Order approval against restaurant availability |
| **Customer Service** | 8184 | Customer data; feeds the order service's read model |

The **Order Service** acts as the SAGA orchestrator: it drives the workflow by publishing payment and approval requests through outbox tables and reacting to the responses.

## 🟦 Hexagonal Architecture (Ports & Adapters)

Each service (except the thin customer service) is split into layers:

- **Domain Core** — technology-agnostic business rules (aggregates, entities, value objects, domain events, domain services)
- **Application Service** — use-case orchestration, input/output ports, SAGA steps, and outbox handling
- **Data Access** — JPA adapters for PostgreSQL
- **Messaging** — Kafka publishers and listeners
- **Container** — the Spring Boot application that wires everything together and exposes the REST API

## 🔁 SAGA + 📦 Outbox

Distributed order workflows are coordinated across services and kept consistent with the transactional Outbox pattern:

1. Order created (`PENDING`) — a payment-request row is written to the order's `payment_outbox` in the same transaction.
2. A scheduler publishes outbox rows to Kafka; the Payment Service validates credit and responds.
3. On payment success the order becomes `PAID` and a restaurant-approval request is written to `restaurant_approval_outbox`.
4. The Restaurant Service approves the order; on success the order becomes `APPROVED`.
5. Any failure triggers the compensating (rollback) path, ending in `CANCELLED`.

The outbox tables guarantee that a database change and its corresponding Kafka event are never out of sync, even across restarts.

## 📡 Kafka + Avro

Kafka carries all service-to-service communication. Message payloads are serialized with Avro against a Confluent Schema Registry.

## 🗄️ Technologies

- Java 17 (see the build note below)
- Spring Boot 2.6.7
- Maven (multi-module reactor)
- Apache Kafka + Zookeeper + Confluent Schema Registry
- PostgreSQL
- Docker / Docker Compose
- JUnit 5 / Mockito

## 🏗️ Module Layout

```
food-ordering-system
├── common
│   ├── common-domain           # shared value objects (Money, ids, statuses)
│   ├── common-application       # shared REST error handling
│   └── common-dataaccess        # shared JPA entities
├── infrastructure
│   ├── kafka                    # kafka config, model, producer, consumer
│   ├── saga                     # SagaStep / SagaStatus abstractions
│   └── outbox                   # outbox scheduling contracts
├── order-service
│   ├── order-domain
│   │   ├── order-domain-core
│   │   └── order-application-service
│   ├── order-dataaccess
│   ├── order-messaging
│   ├── order-application         # REST controllers
│   └── order-container           # Spring Boot app (port 8181)
├── payment-service
│   ├── payment-domain
│   │   ├── payment-domain-core
│   │   └── payment-application-service
│   ├── payment-dataaccess
│   ├── payment-messaging
│   └── payment-container         # Spring Boot app (port 8182)
├── restaurant-service
│   ├── restaurant-domain
│   │   ├── restaurant-domain-core
│   │   └── restaurant-application-service
│   ├── restaurant-dataaccess
│   ├── restaurant-messaging
│   └── restaurant-container      # Spring Boot app (port 8183)
└── customer-service              # single-module Spring Boot app (port 8184)
```

## 🧪 Testing

- **Unit tests** for the highest-risk domain logic: `Money` arithmetic, the `Order` state machine, `OrderSagaHelper` status mapping, `PaymentDomainServiceImpl` credit/ledger rules, and `Restaurant` order validation.
- **Integration tests** in the order and payment containers covering outbox-based idempotency (double-payment handling). These require a running PostgreSQL and Kafka and are not part of the default fast unit run.

Run the unit tests:

```bash
mvn -pl common/common-domain,\
order-service/order-domain/order-domain-core,\
payment-service/payment-domain/payment-domain-core,\
restaurant-service/restaurant-domain/restaurant-domain-core test
```

## ⚙️ Build

> **JDK 17 is required to build.** The project's Lombok version (from Spring Boot 2.6.7) cannot parse the internals of JDK 21+, so building with a newer JDK fails with a Lombok `NoSuchFieldError`. Point `JAVA_HOME` at a JDK 17 install before building.

```bash
export JAVA_HOME=/path/to/jdk-17
mvn install -DskipTests
```

## 🐳 Running

`docker-compose.yml` starts the runtime **infrastructure** (it does not build the service containers):

- PostgreSQL on `5432`
- a 3-broker Kafka cluster on `19092`, `29092`, `39092`
- Zookeeper on `2181`
- Confluent Schema Registry on `8081`

```bash
docker compose up -d
```

Then run each service (each is an executable Spring Boot jar; each initializes its own PostgreSQL schema on startup):

```bash
java -jar customer-service/target/customer-service-1.0-SNAPSHOT.jar
java -jar order-service/order-container/target/order-container-1.0-SNAPSHOT.jar
java -jar payment-service/payment-container/target/payment-container-1.0-SNAPSHOT.jar
java -jar restaurant-service/restaurant-container/target/restaurant-container-1.0-SNAPSHOT.jar
```

Place an order (customer and restaurant ids below come from the seeded data):

```bash
curl -X POST http://localhost:8181/orders \
  -H "Content-Type: application/json" \
  -H "Accept: application/vnd.api.v1+json" \
  -d '{
    "customerId": "d215b5f8-0249-4dc5-89a3-51fd148cfb41",
    "restaurantId": "d215b5f8-0249-4dc5-89a3-51fd148cfb45",
    "price": 50.00,
    "items": [
      {"productId": "d215b5f8-0249-4dc5-89a3-51fd148cfb48", "quantity": 1, "price": 50.00, "subTotal": 50.00}
    ],
    "address": {"street": "123 Main St", "postalCode": "12345", "city": "Lagos"}
  }'
```

Track it with the returned `orderTrackingId` — the status moves `PENDING → PAID → APPROVED` as the saga completes:

```bash
curl http://localhost:8181/orders/{trackingId} -H "Accept: application/vnd.api.v1+json"
```

> **Note on Kafka compression:** the producer config uses `gzip`. The upstream project used `snappy`, whose native library fails to initialize on some platforms (notably Apple Silicon); `gzip` is pure-Java and avoids that dependency.

## 📌 Status

Under active development. The four services above run end to end; more features will be added progressively.
