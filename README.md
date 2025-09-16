🍽️ Food Ordering System — Microservices Architecture

A production-ready Food Ordering System built using Microservices, Domain-Driven Design (DDD), Hexagonal Architecture, SAGA, Outbox Pattern, and Kafka for asynchronous communication.

This project demonstrates how to design and implement a scalable, fault-tolerant, and event-driven distributed system, similar to platforms like Uber Eats or DoorDash.

🚀 Features
🧱 Microservices Architecture

The system is composed of independently deployable services:

Order Service

Customer Service

Restaurant Service

Each service is fully isolated and communicates via events.

🟦 Hexagonal Architecture (Ports & Adapters)

Each service is structured into clear layers:

Domain Core

Application Service

Data Access

Messaging

This allows:

Technology-agnostic domain logic

Clear separation of concerns

Easy testing and maintenance

🧩 Domain-Driven Design (DDD)

The domain layer includes:

Aggregates

Entities

Value Objects

Domain Events

Domain Services

The model captures real business rules for food ordering workflows.

🔁 SAGA Pattern (Choreography)

Distributed order workflows across multiple services are coordinated using events:

Order creation

Customer validation

Restaurant approval

Payment processing

Order completion or cancellation

📦 Outbox Pattern

Ensures reliable publishing of domain events using:

Outbox tables

Event publishing scheduler

Transactionally consistent database + Kafka events

📡 Event-Driven Using Kafka

Kafka is used for:

Service-to-service communication

Event propagation

Distributed transactions

Replayability and debugging

🗄️ Technologies

Java 17+

Spring Boot

Maven

Kafka & Zookeeper

PostgreSQL

Docker / Docker Compose

JUnit / Mockito

🏗️ Architecture Overview
food-ordering-system
├── order-service
│    ├── order-domain-core
│    ├── order-application-service
│    ├── order-data-access
│    └── order-messaging
├── customer-service
│    ├── customer-domain-core
│    ├── customer-application-service
│    ├── customer-data-access
│    └── customer-messaging
└── restaurant-service
├── restaurant-domain-core
├── restaurant-application-service
├── restaurant-data-access
└── restaurant-messaging


Each module has its own domain, input ports, output ports, and adapters.

🧪 Testing

Each module includes:

Unit tests for domain logic

Integration tests for repositories

Event-driven tests for Kafka messaging

🐳 Running with Docker

Start containers:

docker-compose up -d


This runs:

Kafka

Zookeeper

PostgreSQL

Optional service containers

🎯 Goals

This project aims to:

Implement clean modular architecture in real-world microservices

Apply DDD for business logic modeling

Use SAGA for reliable distributed workflows

Ensure data consistency with Outbox Pattern

Build a scalable, maintainable, production-level system

📌 Status

This project is under active development.
More features and services will be added progressively.