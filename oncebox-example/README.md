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

These are the host ports published by `docker-compose.yaml`. Services marked **always** have no
Compose profile; the other services start when their listed profile is enabled.

| Service                | Host port | Profile    |
|------------------------|-----------|------------|
| Producer API           | 8080      | always     |
| Consumer API           | 8081      | always     |
| Traffic Generator      | 8082      | `test`     |
| Producer PostgreSQL    | 5432      | `postgres` |
| Consumer PostgreSQL    | 5433      | `postgres` |
| Producer MySQL         | 3306      | `mysql`    |
| Consumer MySQL         | 3307      | `mysql`    |
| Producer Oracle        | 1521      | `oracle`   |
| Consumer Oracle        | 1522      | `oracle`   |
| Redis (inbox cache)    | 6379      | always     |
| Apache Kafka           | 9092      | `kafka`    |
| RabbitMQ               | 5672      | `rabbit`   |
| RabbitMQ management UI | 15672     | `rabbit`   |
| Prometheus             | 9090      | `test`     |
| Grafana                | 3000      | `test`     |


## Typical Flow

The happy path:

1. Traffic Generator sends HTTP requests to the Producer.
2. Producer:
   - persists business data
   - writes domain events to the outbox table, in the same transaction
3. A background publisher sends the events to Kafka/Rabbit.
4. Consumer processes the events idempotently.

### When something goes wrong

Each path below is a short description of what the example does. The oncebox docs explain the details and the settings.

- **A send fails.** The broker rejects the event or does not confirm it in time, so the publisher counts a failed attempt and puts the event back to `PENDING` with a later `next_retry_at`. The producer allows `max-retries: 3` attempts and waits longer after each failure (backoff `delay: 1m`, `multiplier: 1.5`). See [Retries and backoff](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/processing/#retries-and-backoff).
- **Retries are exhausted.** The event becomes `FAILED` and, because the producer sets `oncebox.outbox.dlq.enabled: true`, a background job moves it to the dead letter queue (the `outbox_dlq_events` table), where it waits to be retried or given up on. See [Dead Letter Queue](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/dlq/).
- **An instance dies mid-send.** An event that a dead or hung publisher captured stays `IN_PROCESS`. The stuck-event recovery job returns it to `PENDING`, without counting an attempt, and it is sent again. See [Stuck event recovery](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/processing/#stuck-event-recovery).
- **The broker delivers a message twice.** Publishing is at-least-once, so a duplicate can reach the consumer. Every copy carries the same event id, and the consumer's inbox skips an id it has already recorded: it checks the Redis cache first and the inbox table on a cache miss. See [Inbox](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/inbox/) and [Duplicates](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/guarantees/#duplicates).
- **Batches.** The batch endpoints (`/api/orders/batch`) change several orders and store their outbox events in the same transaction. `create-order` events go to `orders.created` (a Kafka topic, or a RabbitMQ queue with the `rabbit` profile), where the consumer's batch listener receives them as a list; `update-order` and `delete-order` events go to `orders` and are handled one message at a time. See [Publishing Events](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/publishing/) and [Inbox](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/inbox/).

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

### Outbox Dashboard

The outbox dashboard (`oncebox-outbox`) contains 5 panels displaying metrics from the producer application.

| Panel | Title | Visualization | Queries |
|-------|-------|---------------|---------|
| 1 | **outbox** | Timeseries | `outbox_events` (total)<br>`outbox_events_by_status{status="pending"}`<br>`outbox_events_by_event_type_and_status{status="pending", event_type="create-order"}`<br>`outbox_events_by_action_type_rate_total{action_type="cleaned"}`<br>`outbox_events_rate_total{status="processed"}` |
| 2 | **dlq** | Timeseries | `outbox_dlq_events_by_action_type_rate_total`<br>`outbox_dlq_transfer_to_duration_seconds_max` (hidden)<br>`outbox_dlq_transfer_from_duration_seconds_max` (hidden)<br>`outbox_dlq_events`<br>`outbox_dlq_events_by_status{status="moved"}`<br>`outbox_dlq_events_by_event_type_and_status{status="moved", event_type="create-order"}` |
| 3 | **processing duration** | Timeseries | `oncebox_task_processing_duration_seconds_count` (count of timer observations) |
| 4 | **polling delay** | Timeseries | `oncebox_polling_delay_milliseconds` |
| 5 | **task details** | Pie chart | `oncebox_started_tasks_total{task_type="create-order"}`<br>`oncebox_succeeded_tasks_total{task_type="create-order"}`<br>`oncebox_skipped_tasks_total{task_type="create-order"}`<br>`oncebox_failed_tasks_total{task_type="create-order"}` |

> **Screenshots**: Captured from live Grafana dashboards with producer traffic.
> - ![Outbox Dashboard Overview](docs/outbox-dashboard.png)

---

### Inbox Dashboard

The inbox dashboard (`oncebox-inbox`) contains 3 panels displaying metrics from the consumer application.

| Panel | Title | Visualization | Queries |
|-------|-------|---------------|---------|
| 1 | **inbox** | Timeseries | `inbox_events_total`<br>`inbox_cache_action_total` |
| 2 | **clean up details** | Pie chart | `oncebox_started_tasks_total{task_type="inbox-cleanup"}`<br>`oncebox_succeeded_tasks_total{task_type="inbox-cleanup"}`<br>`oncebox_skipped_tasks_total{task_type="inbox-cleanup"}`<br>`oncebox_failed_tasks_total{task_type="inbox-cleanup"}` |
| 3 | **execution details** | Timeseries | `oncebox_polling_delay_milliseconds{task_type="inbox-cleanup"}`<br>`oncebox_task_processing_duration_seconds_count{task_type="inbox-cleanup"}` (count of timer observations) |

> **Screenshots**: Captured from live Grafana dashboards with consumer traffic.
> - ![Inbox Dashboard Overview](docs/inbox-dashboard.png)

---

## Observability Notes

### Metrics Availability

Metrics are exposed via Prometheus at:
- Producer: `http://localhost:8080/actuator/prometheus` (outbox and DLQ metrics)
- Consumer: `http://localhost:8081/actuator/prometheus` (inbox metrics)

Each application exposes only the metrics relevant to its role.

### Key Metrics Reference

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `outbox_events` | Gauge | — | Total number of outbox events |
| `outbox_events_by_status` | Gauge | `status` (pending, in_process) | Number of outbox events by status |
| `outbox_events_by_event_type_and_status` | Gauge | `event_type`, `status` | Number of outbox events by type and status |
| `outbox_events_rate_total` | Counter | `event_type`, `status` | Outbox events processed (`processed`) |
| `outbox_events_by_action_type_rate_total` | Counter | `action_type` (`attempt_move_to_dlq`, `recovered`, `cleaned`, `success_moved_to_dlq`) | Outbox action lifecycle |
| `outbox_dlq_events` | Gauge | — | Total number of outbox DLQ events |
| `outbox_dlq_events_by_status` | Gauge | `status` (moved, in_process, to_retry) | Number of DLQ events by status |
| `outbox_dlq_events_by_event_type_and_status` | Gauge | `event_type`, `status` | Number of DLQ events by type and status |
| `outbox_dlq_events_by_action_type_rate_total` | Counter | `action_type` (`attempt_move_to_outbox`, `success_moved_to_outbox`, `cleaned`) | DLQ action lifecycle |
| `outbox_dlq_transfer_to_duration_seconds_max` | Timer | — | Max duration of transfer to DLQ |
| `outbox_dlq_transfer_from_duration_seconds_max` | Timer | — | Max duration of transfer from DLQ |
| `inbox_events_total` | Counter | `type` (`consumed`, `rejected_duplicates`, `cleaned`) | Inbox events by outcome |
| `inbox_cache_action_total` | Counter | `action_type` (`hit`, `miss`) | Cache actions |
| `oncebox_task_processing_duration` | Timer (seconds) | `task_type` | Task execution time |
| `oncebox_started_tasks` | Counter | `task_type` | Tasks started |
| `oncebox_succeeded_tasks` | Counter | `task_type` | Tasks completed successfully |
| `oncebox_skipped_tasks` | Counter | `task_type` | Tasks skipped (no work or lock held) |
| `oncebox_failed_tasks` | Counter | `task_type` | Tasks failed |
| `oncebox_polling_delay_milliseconds` | Gauge (ms) | `task_type` | Current polling delay |

### Troubleshooting Guide

| Symptom | Dashboard Panel | Possible Cause | Action |
|---------|-----------------|----------------|--------|
| High pending outbox events | Outbox → outbox panel (`outbox_events_by_status`) | Broker connectivity issues, backpressure, or stuck recovery | Check broker health, DLQ status, recovery logs |
| Growing DLQ count | Outbox → dlq panel (`outbox_dlq_events`) | Failed publishing attempts exhausted retries | Check DLQ action breakdown, resolve root cause |
| High polling delay | Outbox → polling delay panel | Database contention or long transactions | Check DB connection pool, transaction boundaries |
| Low cache hit rate | Inbox → inbox panel (`inbox_cache_action_total`) | Redis unreachable or cache warmup | Check Redis health, TTL settings |
| High duplicate rejection | Inbox → inbox panel (`inbox_events_total{type="rejected_duplicates"}`) | Producer replay or broker misconfiguration | Check producer DLQ, broker delivery guarantees |
| Consumer not processing | Inbox → inbox panel (`inbox_events_total` stagnant) | Listener not connected or broker auth | Check consumer logs, Kafka/RabbitMQ connectivity |

---

> *Generated with assistance from [Claude Code](https://claude.ai/code).*