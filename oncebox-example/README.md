[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7+-DC382D?logo=redis)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9-231F20?logo=apachekafka)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-E6522C?logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800?logo=grafana)

## Overview

A demo project that showcases the [oncebox](https://github.com/dmitriy-iliyov/oncebox) library usage in a microservices architecture.

The `oncebox-example` project is a runnable demo with a **producer** that exposes single and batch REST endpoints 
(`POST`, `PATCH`, `DELETE` on `/api/orders` and `/api/orders/batch`) and a **consumer** with single and batch listeners for both Kafka and RabbitMQ.

The project demonstrates:
- atomicity between business operations and event publishing
- idempotent message consumption
- single-message and batch processing
- basic observability with Prometheus and Grafana

## Architecture Overview

The diagram shows every service started by the `kafka,postgres,test` profile combination. With the `rabbit` profile, RabbitMQ replaces Kafka; with `mysql` or `oracle`, those databases replace PostgreSQL.

```mermaid
%%{init: {"themeVariables": {"fontSize": "20px"}, "flowchart": {"nodeSpacing": 60, "rankSpacing": 70, "padding": 25, "subGraphTitleMargin": {"top": 10, "bottom": 20}}}}%%
flowchart TB
    TG["oncebox-traffic-generator-app<br/>Gatling scenario"]

    subgraph PROD["oncebox-producer-app"]
        API["Order API<br/>create / update / delete"]
        POLL["Oncebox poller<br/>background publisher"]
    end

    PDB[("oncebox-producer-postgresql<br/>orders + outbox table")]
    KAFKA{{"oncebox-kafka<br/>topics: orders, orders.created"}}

    subgraph CONS["oncebox-consumer-app"]
        LST["Kafka listeners<br/>idempotent consumer"]
    end

    REDIS[("oncebox-consumer-redis<br/>inbox cache")]
    CDB[("oncebox-consumer-postgresql<br/>inbox table")]

    PROM["oncebox-prometheus"]
    GRAF["oncebox-grafana"]

    TG -->|"HTTP requests"| API
    API -->|"order + outbox event<br/>in one transaction"| PDB
    POLL -->|"polls pending events"| PDB
    POLL -->|"publishes"| KAFKA
    KAFKA -->|"consumes"| LST
    LST -->|"1. check event id"| REDIS
    REDIS -->|"2. on cache miss"| CDB
    PROM -.->|"scrapes metrics"| API
    PROM -.->|"scrapes metrics"| LST
    GRAF -->|"reads metrics"| PROM
```

Components:
- **oncebox-traffic-generator-app**: a Gatling scenario that sends order requests to the producer to generate load.
- **oncebox-producer-app**: serves the order API (create, update, delete) and saves each order together with its outbox event in one transaction. Its oncebox poller publishes pending events to the broker.
- **oncebox-producer-postgresql**: the producer's own database, holding the orders and the outbox table.
- **oncebox-kafka**: the message broker, carrying the `orders` and `orders.created` topics (RabbitMQ queues with the `rabbit` profile).
- **oncebox-consumer-app**: listens to both topics, handles single and batch messages, and skips events it has already processed. Its handlers only log the events.
- **oncebox-consumer-redis**: caches the ids of consumed events, checked before the database.
- **oncebox-consumer-postgresql**: the consumer's own database, holding the inbox table of processed event ids.
- **oncebox-prometheus**: scrapes metrics from the producer and consumer apps.
- **oncebox-grafana**: shows dashboards built from the Prometheus data.

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
3. A background publisher sends events to Kafka/Rabbit
4. Consumer processes events idempotently

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

## Dashboards

Grafana comes provisioned: the Prometheus datasource is pinned to the `oncebox-prometheus` uid and
the dashboards in [`docs/grafana`](../docs/grafana) are loaded from disk on start. Open
<http://localhost:3000> (`admin` / `admin`) and they are already there — nothing to import by hand.

The dashboards pick their datasource through a `datasource` variable instead of a hardcoded uid, so
the same files can also be imported into any other Grafana; pick the Prometheus instance from the
dropdown at the top of the dashboard.