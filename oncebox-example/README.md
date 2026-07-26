[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7+-DC382D?logo=redis)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-231F20?logo=apachekafka)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-E6522C?logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800?logo=grafana)

## Overview

A demo project that showcases the [oncebox](https://github.com/dmitriy-iliyov/oncebox) library usage in a microservices architecture.

The project demonstrates:
- atomicity between business operations and event publishing
- idempotent message consumption
- single-message and batch processing
- basic observability with Prometheus and Grafana

## Architecture Overview

The system consists of three main components:
1. Producer Service
- handles HTTP requests for orders: create, update, delete 
- persists business data in PostgreSQL 
- stores domain events in the outbox table within the same transaction 
- publishes events asynchronously to Kafka
2. Consumer Service
- subscribes to Kafka topics for order events 
- processes events idempotently 
- supports single and batch consumption 
- maintains internal state
3. Traffic Generator 
- Gatling scenario

## Ports 

| Service           | Port |
|-------------------|------|
| Producer API      | 8080 |
| Consumer API      | 8081 |
| Traffic Generator | 8082 |
| Apache Kafka      | 9092 |
| RabbitMQ          | 5672 |
| Prometheus        | 9090 |
| Grafana           | 3000 |


## Typical Flow

1. Traffic Generator sends HTTP requests to the Producer
2. Producer:
- persists business data 
- writes domain events to the outbox table
4. A background publisher sends events to Kafka/Rabbit
5. Consumer processes events idempotently

## Run
From the `oncebox-example` directory (the build context is the repository root, so the library
itself is built from source):

1. Build
```bash
docker compose build
```
2. Run
- with Apache Kafka:
```bash
COMPOSE_PROFILES=kafka,postgres,test docker compose up
```

- with RabbitMQ:
```bash
COMPOSE_PROFILES=rabbit,postgres,test docker compose up
```

> [!NOTE]
> For test with other database use `mysql` or `oracle`. The profile `test` using for enable traffic generator and observability container.  