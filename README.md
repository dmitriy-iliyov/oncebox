[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox)
[![codecov](https://codecov.io/github/dmitriy-iliyov/oncebox/branch/main/graph/badge.svg?token=8X6B9K3AOK)](https://codecov.io/github/dmitriy-iliyov/oncebox)
[![CI](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/ci.yml)
[![E2E](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/e2e.yml/badge.svg)](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/e2e.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/oncebox-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/oncebox-starter)
[![javadoc](https://javadoc.io/badge2/io.github.dmitriy-iliyov/oncebox-core/javadoc.svg)](https://javadoc.io/doc/io.github.dmitriy-iliyov/oncebox-core)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/oncebox)
[![GitHub Release Date](https://img.shields.io/github/release-date/dmitriy-iliyov/oncebox)](https://github.com/dmitriy-iliyov/oncebox/releases/latest)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/oncebox)

## Overview
This library provides an implementation of the [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html) based on polling events from a relational database and publishing them to a message broker.

The library offers flexible configuration for processing different event types.
Each event type is handled independently, and events are processed in parallel using a configurable thread pool, allowing predictable throughput and controlled resource usage.

This approach ensures reliable event publication without relying on database log-based CDC solutions, making it suitable for environments where simplicity, portability, and explicit control over event processing are preferred.

## Key Features
- **Atomic persistence** - events are stored within the same business transaction, ensuring consistency.
- **Adaptive polling** - dynamically adjusts the polling interval based on workload to optimize database usage.
- **Flexible polling configuration** - allows customization of polling intervals, retry policies, and backoff strategies per event type.
- **At-least-once delivery** - guarantees delivery with configurable retries and backoff policies.
- **Effectively-once delivery** - achieved via an idempotent consumer implementation provided by the library.
- **Dead Letter Queue** - stores events that fail after all retry attempts, with REST API support for management.
- **Background service** - handles recovery of stuck events and cleanup of processed or consumed events automatically.
- **Observability** - provides out-of-the-box metrics integration via Micrometer.

## Supported Infrastructure

- **Databases:** PostgreSQL 16+, MySQL 8+, Oracle 23+.
- **Message Brokers:** Apache Kafka 3.7+, RabbitMQ 3.12+.
- **Cache Storage (optional for caching):** Redis 7+.

## Limitations
- **Horizontal scaling** - performance degradation may occur when reaching a certain number of instances due to `SKIP LOCKED` - based concurrent polling mechanism. The optimal number of instances depends on the number of event types and database load.
- **No ordering guarantees** - events are processed in parallel by type, with no guaranteed delivery order within or across event types.
- **MySQL requires READ COMMITTED** - see the note below.

> [!IMPORTANT]
> **MySQL: run the outbox datasource at `READ COMMITTED`.**
> Under InnoDB's default `REPEATABLE READ`, the `SELECT ... FOR UPDATE SKIP LOCKED` polling query takes
> next-key (gap) locks, so several instances polling the same table concurrently deadlock instead of
> each grabbing a disjoint batch. `READ COMMITTED` disables gap locking and lets the pollers scale out.
> An outbox poller never needs repeatable reads, so this is safe. PostgreSQL and Oracle are unaffected.

## Quick Start

1. Add dependencies

`oncebox-starter` is always required:
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-starter</artifactId>
      <version>1.1.2</version>
  </dependency>
```

> [!IMPORTANT]
> `oncebox-starter` does not bundle a database dialect by default - you must explicitly add exactly
> one of `oncebox-postgresql`, `oncebox-mysql`, or `oncebox-oracle`, matching your database. Without
> it, the library has no DAO implementation to wire up and the application context will fail to start.
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-postgresql</artifactId>
      <version>1.1.2</version>
  </dependency>
```

You also need exactly one message transport module, matching your broker:

For Apache Kafka:
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-kafka</artifactId>
      <version>1.1.2</version>
  </dependency>
```

For RabbitMQ:
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-rabbit</artifactId>
      <version>1.1.2</version>
  </dependency>
```

Optionally, you can also add:

`oncebox-metrics` to enable the metrics collecting, read more [here](#observability).
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-metrics</artifactId>
      <version>1.1.2</version>
  </dependency>
```

`oncebox-dlq-api` to enable the REST API for manual DLQ management, read more [here](#dlq-rest-api).
No extra dialect dependency is needed - it reuses the one you already added above.
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-dlq-api</artifactId>
      <version>1.1.2</version>
  </dependency>
```

2. Enable the starter on the publisher side:
```java
@SpringBootApplication 
@EnableTransactionManagement
@EnableKafka
@EnableOutbox
public class PublisherRunner {

    public static void main(String ... args) {
        SpringApplication.run(PublisherRunner.class, args);
    }
}
```

3. Minimal YAML config:

> [!WARNING]
> DLQ is disabled by default.

```yaml
oncebox:
  publisher:
    sender:
      type: kafka              # or rabbit
    events:
      create-example-event:
        topic: "example-events"
```

Read more about the configuration [here](#publisher-2).

4. Inject `OutboxPublisher`:
```java 
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository repository;
    private final ExampleMapper mapper;
    private final OutboxPublisher outboxPublisher;
    
    @Transactional
    public ExampleDto save(ExampleCreateDto dto) {
        ExampleEntity entity = repository.save(mapper.toEntity(dto));
        ExampleDto response = mapper.toDto(entity);
        outboxPublisher.publish("create-example-event", response);
        return response;
    }
}
```

Or use the `@OutboxPublish` annotation:

```java
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository repository;
    private final ExampleMapper mapper;

    @Transactional
    @OutboxPublish(eventType = "create-example-event")               // The annotation uses the method result as the payload by default
    public ExampleDto save(ExampleCreateDto dto) {
        ExampleEntity entity = repository.save(mapper.toEntity(dto));
        return mapper.toDto(entity);
    }
}
```
5. Add dependencies on the consumer side:

```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-starter</artifactId>
      <version>1.1.2</version>
  </dependency>
```

You can also add `oncebox-consumer-cache` to enable the cache on consumer side, read more [here](#idempotent-processing).
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>oncebox-consumer-cache</artifactId>
      <version>1.1.2</version>
  </dependency>
```

6. Enable the starter on the consumer side by adding `@EnableOutbox` to your main class.
7. Minimal YAML config:

> [!NOTE]
> Cleanup is enabled by default, cache is enabled once `cache.cache-name` is set.

```yaml
oncebox:
  publisher:
    enabled: false
  consumer:
    source:
      type: kafka                  # or rabbit
    mappings:
      # Your specific mappings    
    enabled: true
    cache:
      cache-name: "<service-name>:oncebox:consumed"
```

Read more about the configuration [here](#consumer-2).

8. Create a listener with an injected `OutboxIdempotentConsumer`:

**Apache Kafka:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExampleKafkaListener {

    private final OutboxIdempotentConsumer outboxConsumer;
    
    @KafkaListener(topics = "example-events", groupId = "example-group", containerFactory = "outboxKafkaListenerContainerFactory")
    public void listen(Message<ExampleDto> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                m -> handlers.get(OutboxHeadersUtils.extractEventType(m)).accept(m.getPayload())
        );
    }
}
```

**RabbitMQ:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExampleRabbitListener {

    private final OutboxIdempotentConsumer outboxConsumer;

    @RabbitListener(queues = "example-events", containerFactory = "outboxRabbitListenerContainerFactory")
    public void listen(Message<ExampleDto> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                m -> handlers.get(OutboxHeadersUtils.extractEventType(m)).accept(m.getPayload())
        );
    }
}
```

Read more about event metadata [here](#event-headers)

---

## Example & Test Environment

Example of full configured project with simple traffic generator is [here](https://github.com/dmitriy-iliyov/oncebox/tree/main/oncebox-example). Project is fully containerised with Docker.

## Design

### Architecture Overview

The system architecture with full configuration looks as follows:

![outbox-architecture](docs/images/outbox-architecture.png)

The event lifecycle follows these steps:
1. Event or batch of events is created
2. Scheduled procedure polls a batch of events by type
3. Attempt to publish the batch:
    - On success: batch is marked as processed
    - On failure: configured number of retries is performed for this event type
        - When retries are exhausted: event is atomically moved to DLQ
4. Event is placed in the message queue

The following flow applies only when using the idempotent consumer:
1. Consumer receives an event or batch of events
2. Consumption check is performed (all subsequent steps are atomic):
    - If event was already consumed: business action is not executed and no exceptions are thrown
        - For batch consumption: events are filtered and business operation executes only for unconsumed events
    - If event was not consumed: event is marked as consumed and business operation is executed

---

### Polling Mechanism

The polling mechanism is based on selecting events by type, sorted by creation date (or by retry date after the first failed delivery attempt).
Events of different types can be processed in parallel using threads from a dedicated library-managed thread pool (you can manage this with `thread-pool-size`).
Detailed configuration options and recommendations for pool size are [here](#global).

To enable parallel event processing across multiple application instances, the library uses `FOR UPDATE SKIP LOCKED`, which allows concurrent processing of event batches. This architectural decision has two drawbacks:
- Performance degrades as the number of instances increases due to longer search times for available event batches
- Event processing order cannot be guaranteed

The library provides two polling strategies: `fixed` and `adaptive`.

The `fixed` strategy uses a constant delay between polling iterations, making it predictable and easier to reason about under stable workloads. 

The `adaptive` strategy dynamically adjusts the delay between polling iterations using exponential backoff. When no events are available, the delay increases (up to `max-fixed-delay`) to reduce unnecessary database load. As soon as events are detected, the delay is reset to `min-fixed-delay`, allowing the system to react quickly to new workload.

Detailed polling configuration options are available [here](#polling).

---

### Delivery Semantics

> [!WARNING]
> The library expects the message broker to be configured by the developer. 
> To ensure the delivery semantics described below, the broker must be configured with `acks` (or an equivalent mechanism). 
> If the broker uses a producer-side buffer, its settings should also be tuned appropriately for the expected load.

#### At-Least-Once

This delivery semantic is ensured without consumer-side configuration. The publisher waits for successful acknowledgment from the queue confirming event receipt.

If the publisher crashes after polling but before sending to the queue, events become stuck in `IN_PROCESS` state. They will be detected by a dedicated worker and transitioned back to `PENDING` state without counting the failed delivery attempt.

More details about the state machine [here](#event-state-machine).

#### Exactly-Once
This semantic is achievable only when using the idempotent consumer, which deduplicates messages based on event identifiers generated when the event is initially saved at the beginning of its lifecycle.

In reality, the term "Exactly-Once" is not entirely accurate and should be understood as "Effectively-Once", since the library cannot guarantee deduplication within the broker itself.

---

### Publisher

#### Usage
There are two options for use:
- manual using `OutboxPublisher#publish()`
```java
public interface OutboxPublisher {
  <T> void publish(String eventType, T event);
  <T> void publish(String eventType, List<T> events);
}
```
- using an `@OutboxPublish` annotation
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OutboxPublish {
  String eventType();
  String payload() default "#result";
}
```
Both approaches require specifying the eventType in accordance with the YAML configuration and support saving events in batches.
When using the annotation-based approach, the payload can be derived from method parameters or the return value and is configured via SpEL expressions (e.g. by referencing a parameter name).

More usage examples [here](https://github.com/dmitriy-iliyov/oncebox/blob/main/oncebox-example/oncebox-producer-example/src/main/java/io/github/dmitriyiliyov/oncebox/example/producer/OrderService.java).

---

#### Event State Machine
Events transition through the following states:
```text
PENDING → IN_PROCESS → PROCESSED (future cleanup)
               ↓
             FAILED (after max retries exhausted, future transfer to DLQ)
```
- `PENDING`: event created and waiting to be polled
- `IN_PROCESS`: event currently being processed by a publisher instance, if the publisher crashes, the event will remain in this state. This is an intermediate state that signals that an event has been accepted for processing by a worker. All workers except for [stuck event recovery](#stuck-event-recovery) do not work with this status. The state is assigned after a transaction locks a row in the database via FOR UPDATE.
- `PROCESSED`: event successfully sent to message broker
- `FAILED`: event failed after exhausting all retry attempts

---

#### Transactional Guarantees
The library ensures atomic storage of outbox events within the same database transaction as business entity modifications. This guarantees that either both the business change and the event are saved, or neither is saved, preventing inconsistencies between the database and message broker.

All methods for saving simultaneously with a business event are annotated with `@Transactional(propagation = Propagation.MANDATORY)`, which means that the transaction is mandatory and is opened by the client code.

> [!WARNING]
> When using the `@OutboxPublish` annotation together with `@Transactional` on the same method, event
> persistence relies on the library's AOP aspect running before the transaction commits. This works
> under Spring Boot's default (unconfigured) transaction advisor precedence. If your application
> customizes `@EnableTransactionManagement(order = ...)` to a higher-precedence value than the default,
> the aspect may end up running after commit and publishing will fail with
> `IllegalTransactionStateException`. In that case, use manual `OutboxPublisher#publish()` inside the
> transactional method instead of the annotation.

---

#### Serialization

The library uses Jackson-based serialization by default, but if necessary, you can define your own by implementing `OutboxSerializer`:
```java
public interface OutboxSerializer {
    
    <T> OutboxEvent serialize(String eventType, T event);
    
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
```

Jackson is an optional dependency of `oncebox-core` - it is `oncebox-starter` that brings it in, through
`spring-boot-starter-json`. If you provide your own `OutboxSerializer` and exclude that starter, Jackson
stays off your classpath entirely.

---

#### Event Ids

Every outbox event gets a **UUIDv7** identifier - a time-ordered UUID, so inserts hit the tail of the
primary key index instead of scattering across it, which matters for a table with this write volume.

The ids are produced by the default `UuidGenerator` bean:
```java
public interface UuidGenerator {
    UUID generate();
}
```

> [!NOTE]
> The default implementation delegates to [`com.github.f4b6a3:uuid-creator`](https://github.com/f4b6a3/uuid-creator),
> a required dependency of `oncebox-core`, so it lands on your classpath transitively. It is a small
> (~140 KB), MIT licensed library with no runtime dependencies of its own. Registering your own
> `UuidGenerator` bean overrides the generation, but does not remove the dependency.

Keep in mind that the id is also what makes consumer-side idempotency work, so a custom generator must
still produce globally unique values.

---

#### Retry Policy
An exponential backoff is supported where the delay increases exponentially with each retry, the next retry attempt is calculated using the following formula:

`next_retry_at = initial_delay * (multiplier ^ retry_attempt)`

Example: 10s -> 30s -> 90s -> 270s...

Detailed configuration options are available [here](#defaults--events).

---

#### Stuck Event Recovery
Events that are in the `IN_PROCESS` state for more than the specified threshold (`max-batch-processing-time` in the configuration) are considered stuck, they are caught by the worker and transferred to the `PENDING` state **without increasing the number of failed attempts**.

Detailed configuration options are available [here](#stuck-event-recovery-1).

---

#### Cleanup

Outbox events with the `PROCESSED` status are periodically cleaned up by a background worker according to the configured retention policy.

When cleaning, distributed locking is used to ensure that only one instance will perform the work, this should be taken into account when setting up.

Detailed configuration options are available [here](#cleanup-3).

---

#### Dead Letter Queue

The Dead Letter Queue is implemented using the database. As described above, events are automatically moved to the DLQ once they are marked with the `FAILED` status after the maximum number of retry attempts is exceeded.

Events placed into the DLQ must be **manually reviewed**. After review, an event can be marked as either `TO_RETRY` or `RESOLVED`.  
To support this workflow, the library provides a **REST API** for DLQ management with the following capabilities.

Each DLQ event follows a defined state machine:
```text
MOVED → IN_PROCESS → RESOLVED (future cleanup or manually delete)
            ↓
         TO_RETRY (returned to outbox_events and deleted from outbox_dlq_events)
```
State definitions:

- `MOVED`: the event has been transferred from `outbox_events` to the DLQ and is waiting for manual review
- `IN_PROCESS`: the event is currently being processed by a DLQ transfer worker
- `TO_RETRY`: the event has been reviewed and approved for retry; it will be moved back to `outbox_events`
- `RESOLVED`: the event is considered permanently failed and will not be retried

##### DLQ Transfer Semantics

Event transfers between `outbox_events` and `outbox_dlq_events` are performed **within a single database transaction** to ensure consistency:

- start transaction
    - load a batch of DLQ events and lock them by setting status to `IN_PROCESS` to prevent concurrent handling
    - insert events into the target table
    - delete the corresponding batch from the source table
- commit transaction

This guarantees atomicity and prevents event duplication or loss during DLQ transitions.

There is a handler interface that is invoked when events are transferred from the `outbox_events` table to the `outbox_dlq_events` table.
It can be used to integrate alerting or monitoring.
By default, it simply logs the events moved to the DLQ.

```java
public interface OutboxDlqHandler {
    void handle(List<OutboxEvent> events);
}
```

##### DLQ REST API
The Dead Letter Queue provides a REST API for managing events that have failed delivery or require manual review.

> [!WARNING]
> You should secure DLQ REST API paths.

| Method                                                | Path                           | Params                                                                                | Request Body                                                         | Description                                                                                           |
|:------------------------------------------------------|:-------------------------------|:--------------------------------------------------------------------------------------|:---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| ![GET](https://img.shields.io/badge/GET-4CAF50)       | `/api/outbox-dlq/events/{id}`  | id: UUID                                                                              | —                                                                    | Get DLQ event by ID                                                                                   |
| ![GET](https://img.shields.io/badge/GET-4CAF50)       | `/api/outbox-dlq/events/batch` | status: DlqStatus, <br/>eventType: String, <br/>batchNumber: int, <br/>batchSize: int | —                                                                    | Get batch of DLQ events by status or eventType, returnig without sort all if the parameter is missing |
| ![GET](https://img.shields.io/badge/GET-4CAF50)       | `/api/outbox-dlq/events/count` | status: DlqStatus, <br/>eventType: String                                             | —                                                                    | Get the number of DLQ by status or eventType, count all if the parameter is missing                   |
| ![PATCH](https://img.shields.io/badge/PATCH-9C27B0)   | `/api/outbox-dlq/events/{id}`  | id: UUID                                                                              | status: DlqStatus                                                    | Update single DLQ event status                                                                        |
| ![PATCH](https://img.shields.io/badge/PATCH-9C27B0)   | `/api/outbox-dlq/events/batch` | —                                                                                     | ids: Set&lt;UUID&gt;, <br/>eventType: String, <br/>status: DlqStatus | Update batch DLQ events status by status or eventType, not both                                       |
| ![DELETE](https://img.shields.io/badge/DELETE-F44336) | `/api/outbox-dlq/events/{id}`  | id: UUID                                                                              | —                                                                    | Delete single DLQ event                                                                               |
| ![DELETE](https://img.shields.io/badge/DELETE-F44336) | `/api/outbox-dlq/events/batch` | —                                                                                     | ids: Set&lt;UUID&gt;, <br/>eventType: String                         | Delete batch of DLQ events by status or eventType, not both                                           |

Detailed DLQ configuration options are available [here](#dead-letter-queue-1).

##### Cleanup
DLQ events with the `RESOLVED` status are periodically cleaned up by a background worker according to the configured retention policy.

When cleaning, distributed locking is used to ensure that only one instance will perform the work, this should be taken into account when setting up.

Detailed configuration options are available [here](#cleanup-3).

---

### Consumer

#### Usage
The consumer side of the library provides only manual invocation through the following interface:

```java
public interface OutboxIdempotentConsumer {

    void consume(UUID eventId, Runnable operation);

    <T> void consume(T message, OutboxEventIdExtractor<T> idExtractor, Consumer<T> operation);

    void consume(Set<UUID> ids, Consumer<Set<UUID>> operation);
    
    <T> void consume(List<T> messages, OutboxEventIdExtractor<T> idExtractor, Consumer<List<T>> operation);
}
```

More usage examples [here](https://github.com/dmitriy-iliyov/oncebox/blob/main/oncebox-example/oncebox-consumer-example/src/main/java/io/github/dmitriyiliyov/oncebox/example/consumer/OrderAnalyticKafkaListener.java)

---

#### Idempotent Processing
Idempotent processing is implemented through unique event identifiers that are stored in a dedicated table when consumed. Storage occurs atomically together with the business effect.

As mentioned earlier, when consuming a batch of events, already-consumed events are filtered out and only unconsumed events are passed to the lambda expression.

For improved performance, consumed event identifiers can be cached in a distributed cache. When using this feature, ensure that cleanup of successfully processed events occurs after cache cleanup to prevent data loss.
The library uses `CacheManager` from the application context. Cache configuration must be provided by the developer.

> [!CAUTION]
> **The cache name must be unique for each service.** A cache hit skips the operation without reaching the
> database, and the cache key is the event id alone. Services that consume the same event and share a cache
> store (for example, one Redis) under the same `cache-name` see each other's entries: the first service to
> consume an event marks it for all of them, and every other service silently skips it. Prefix the name with
> the service name (`<service-name>:oncebox:consumed`), or give each service its own key prefix
> (`spring.cache.redis.key-prefix`) or its own cache store.

---

#### Event Headers

> [!IMPORTANT]
> The library uses message headers to pass event metadata for idempotency checks and routing.

The event types themselves act as routing keys in both cases. For **Apache Kafka**, they are placed in event headers and are available for consumer-side routing. For **RabbitMQ**, they are used by the library itself to determine the queue to which the event will be sent and are also available in the headers.

All events published through the outbox pattern include the following headers.

| Header Name                 | Constant                           |  Type  | Description                                                                              |
|-----------------------------|------------------------------------|:------:|------------------------------------------------------------------------------------------|
| `outbox_event_type`         | `OutboxHeaders.EVENT_TYPE`         | String | Enables event dispatching when multiple event types share a topic/exchange               |
| `outbox_event_id`           | `OutboxHeaders.EVENT_ID`           |  UUID  | Unique event identifier for idempotency, used by OutboxIdempotentConsumer implementation |                                                                                               |
| `outbox_event_payload_type` | `OutboxHeaders.EVENT_PAYLOAD_TYPE` | String | Event class for additional dispatching on consumer side                                  |                                                                                               |

---

#### Deserialization

Since the publisher sends events without knowing their specific type and without deserializing them from the JSON format 
in which they are stored in the table the events arrive at the consumer in a raw format. 
The `mappings` configuration allows you to specify which events should be deserialized into which specific class.

Read more about configuration [here](#mappings)

---

#### Cleanup
Consumed outbox events are periodically cleaned up by a background worker according to the configured retention policy.

When cleaning, distributed locking is used to ensure that only one instance will perform the work, this should be taken into account when setting up.

Detailed configuration options are available [here](#cleanup-4).

---

## Observability
### Publisher

> [!NOTE]
> The term 'tasks' encompasses both internal outbox background processes and actual event processing.

**Gauges**

| Metric Name                                  | Description                             | Tags                                                                                                                                                 |
|:---------------------------------------------|:----------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `outbox_events`                              | Total number of outbox events           | —                                                                                                                                                    |
| `outbox_events_by_status`                    | Number of outbox events by status       | `status={pending, in_process}`                                                                                                                       |
| `outbox_events_by_event_type_and_status`     | Number of outbox events by type         | `event_type`, <br/>`status={pending, in_process}`                                                                                                    |
| `outbox_dlq_events`                          | Total number of events in DLQ           | —                                                                                                                                                    |
| `outbox_dlq_events_by_status`                | Number of DLQ events by status          | `status={moved, in_process, to_retry}`                                                                                                               |
| `outbox_dlq_events_by_event_type_and_status` | Number of DLQ events by type and status | `event_type`, <br/>`status={moved, in_process, to_retry}`                                                                                            |
| `outbox_polling_delay_milliseconds`          | Current delay between tasks execution   | `task_type={cleanup-processed-events, stuck-event-recovery, transfer-to-dlq, transfer-from-dlq, cleanup-resolved-dlq-events}` or declared event type |

All event related gauges execute `COUNT` queries against the database and therefore reflect the **exact number of events at the current moment**.

To avoid excessive database load caused by Prometheus scraping, gauge values are **cached by default**.
Caching can be disabled via metrics configuration.

**Counters**

| Metric Name                                                                                                                      | Description                    | Tags                                                                                                                                                 |
|:---------------------------------------------------------------------------------------------------------------------------------|:-------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `outbox_events_rate_total`                                                                                                       | Processed events rate          | `event_type`, <br/>`status={processed}`                                                                                                              |
| `outbox_events_by_action_type_rate_total`                                                                                        | Internal lifecycle events rate | `action_type={attempt_move_to_dlq, recovered, cleaned, success_moved_to_dlq}`                                                                        |
| `outbox_dlq_events_by_action_type_rate_total`                                                                                    | DLQ operational events rate    | `action_type={attempt_move_to_outbox, success_moved_to_outbox, manual_deleted, cleaned}`                                                             |
| `outbox_started_tasks_total`<br/>`outbox_skipped_tasks_total`<br/>`outbox_succeeded_tasks_total`<br/>`outbox_failed_tasks_total` | Rate of task execution states  | `task_type={cleanup-processed-events, stuck-event-recovery, transfer-to-dlq, transfer-from-dlq, cleanup-resolved-dlq-events}` or declared event type |

**Timers**

| Metric Name                       | Description                 | Tags                                                                                                                                                 |
|:----------------------------------|:----------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `outbox_task_processing_duration` | Duration of task processing | `task_type={cleanup-processed-events, stuck-event-recovery, transfer-to-dlq, transfer-from-dlq, cleanup-resolved-dlq-events}` or declared event type |

These timers help identify performance bottlenecks during bulk recovery or DLQ reprocessing operations.

---

### Consumer

**Gauges**

| Metric Name                         | Description                           | Tags                                  |
|:------------------------------------|:--------------------------------------|:--------------------------------------|
| `outbox_polling_delay_milliseconds` | Current delay between tasks execution | `task_type={cleanup-consumed-events}` |


**Counters**

| Metric Name                                                                                                                      | Description                                        | Tags                                                    |
|:---------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------|:--------------------------------------------------------|
| `consumed_outbox_events_total`                                                                                                   | Number of consumed outbox events by specific type  | `type={rejected_duplicates, consumed, cleaned, failed}` |
| `consumed_outbox_cache_action_total`                                                                                             | Number of hit/miss in consumed outbox events cache | `action_type={hit, miss}`                               |
| `outbox_started_tasks_total`<br/>`outbox_skipped_tasks_total`<br/>`outbox_succeeded_tasks_total`<br/>`outbox_failed_tasks_total` | Rate of task execution states                      | `task_type={cleanup-consumed-events}`                   |

**Timers**

| Metric Name                       | Description                 | Tags                                  |
|:----------------------------------|:----------------------------|:--------------------------------------|
| `outbox_task_processing_duration` | Duration of task processing | `task_type={cleanup-consumed-events}` |

## Configuration

### Global

#### Thread Pool Size
When calculating the thread pool size, it's important to account for all background system processes. 

Publisher requires two threads for its background operations (stuck event recovery and cleanup), three more when DLQ is enabled (transfer to DLQ, transfer from DLQ and DLQ cleanup), 
plus one additional thread for each configured event type. Therefore, the recommended number of threads is `2 + n` without DLQ and `5 + n` with DLQ, where `n` is the number of event types.

Consumer side requires only one thread for cleanup as background operation, the number of threads is `1`.

#### Distributed Lock

Used to guarantee that the clean-up job runs on only one instance at a time. You can set global values for all jobs or use adaptive calculation based on the polling mechanism by setting `resolve-by-polling-properties=true`.

```yaml
oncebox:
  thread-pool-size: 5
  tables:
    auto-create: true
  distributed-lock:
    lock-at-least-for: 1s
    lock-at-most-for: 1m
    resolve-by-polling-properties: false
```

| Property                                         | Description                                                                                                                                                                                                                                                               | Default                        |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------|
| `thread-pool-size`                               | Size of the thread pool for parallel event processing                                                                                                                                                                                                                     | `min(available_processors, 5)` |
| `tables.auto-create`                             | Automatically create outbox tables on startup. Create 4 tables: <br/>- `outbox_events` and `outbox_jobs`; <br/>- `outbox_dlq_events` (when `oncebox.publisher.dlq.enabled` is `true`); <br/>- `outbox_consumed_events` (when `oncebox.consumer.enabled` is `true`).       | `true`                         |
| `distributed-lock.lock-at-least-for`             | Minimum time duration between locks. Used when `resolve-by-polling-properties` is false.                                                                                                                                                                                 | `1s`                           |
| `distributed-lock.lock-at-most-for`              | Maximum time duration between locks, the lock can be taken by another instance even if the holder has not released it. Used when `resolve-by-polling-properties` is false.                                                                                        | `1m`                           |
| `distributed-lock.resolve-by-polling-properties` | When this property is enabled, `lock-at-least-for` and `lock-at-most-for` are calculated as follows: <br/> - if `polling.type` of clean-up is `fixed`, they are based on `fixed-delay`; <br/> - if `adaptive`, they are based on `min-fixed-delay` and `max-fixed-delay`. | `true`                         |

### Publisher

#### Sender
```yaml
oncebox:
  publisher:
    sender:
      type: kafka
      bean-name: "customKafkaTemplate"
      emergency-timeout: 120s
```

| Property            | Description                                  | Default                                               |
|---------------------|----------------------------------------------|:------------------------------------------------------|
| `type`              | Message broker type (`kafka` or `rabbit`)    | —                                                     |
| `bean-name`         | Custom sender bean name for multiple senders | Resolved by Java type according to `type`             |
| `emergency-timeout` | Maximum time to wait for a send operation    | `120s`                                                |

---

#### Polling
The library supports two polling strategies: `fixed` and `adaptive`.

Example configuration for `adaptive` polling:
```yaml
polling: 
  type: adaptive
  initial-delay: 300s
  min-fixed-delay: 1s
  max-fixed-delay: 1m
  multiplier: 2.0
```

Example configuration for `fixed` polling:
```yaml
polling: 
  type: fixed
  initial-delay: 300s
  fixed-delay: 2s
```

| Property          | Description                                                            |
|-------------------|------------------------------------------------------------------------|
| `type`            | Polling type (`fixed` or `adaptive`)                                   |
| `initial-delay`   | Delay before first polling starts                                      |
| `fixed-delay`     | Fixed delay between polling iterations (used for `fixed` polling type) |
| `min-fixed-delay` | Min delay between polling iterations                                   |
| `max-fixed-delay` | Max delay between polling iterations                                   |
| `multiplier`      | Multiplier for exponential backoff between polling iterations          |

> [!NOTE]
> Polling does not have global defaults. Each property group — such as defaults, current event, cleanup, stuck recovery, and DLQ transfers — has its own polling default values.

---

#### Defaults & Events

The `defaults` section defines default values that apply to all events unless overridden in individual event configuration.
```yaml
oncebox:
  publisher:
    defaults:
      batch-size: 200
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 250ms
        max-fixed-delay: 1m
        multiplier: 1.5
      max-retries: 3
      backoff:
        enabled: true
        delay: 10s
        multiplier: 3.0
```

| Property                  | Description                                                                                                          |  Default   |
|---------------------------|----------------------------------------------------------------------------------------------------------------------|:----------:|
| `batch-size`              | Number of events to process per iteration                                                                            |   `200`    |
| `polling.type`            | Polling type (`fixed` or `adaptive`)                                                                                 | `adaptive` |
| `polling.initial-delay`   | Delay before first polling starts                                                                                    |    `5m`    |
| `polling.min-fixed-delay` | Min delay between polling iterations                                                                                 |  `250ms`   |
| `polling.max-fixed-delay` | Max delay between polling iterations                                                                                 |    `1m`    |
| `polling.multiplier`      | Multiplier for exponential backoff between polling iterations                                                        |   `1.5`    |
| `max-retries`             | Maximum retry attempts before moving to DLQ                                                                          |    `3`     |
| `backoff.enabled`         | Enable exponential backoff for retries                                                                               |   `true`   |
| `backoff.delay`           | Initial backoff delay                                                                                                |   `10s`    |
| `backoff.multiplier`      | Multiplier for exponential backoff                                                                                   |   `3.0`    |

Individual event configurations override defaults for specific event types.

Apache Kafka example:
```yaml
oncebox:
  publisher:
    events:
      create-order:
        topic: orders
        batch-size: 500
        polling:
          min-fixed-delay: 1s
        max-retries: 2
        backoff:
          delay: 5s
          multiplier: 2.0

      update-order:
        topic: orders
        batch-size: 200
        polling:
          type: fixed
          initial-delay: 10m
          fixed-delay: 250ms
        backoff:
          enabled: false  # Use fixed delay instead

      delete-order:
        topic: orders
        batch-size: 500
        polling:
          max-fixed-delay: 5m
          multiplier: 5.0
```
| Property | Description                                           |
|----------|-------------------------------------------------------|
| `topic`  | Destination topic (Kafka) or exchange (RabbitMQ) name |

All other parameters same as `defaults` section, but override defaults for this specific event type

RabbitMQ example:
```yaml
oncebox:
  publisher:
    events:
      create-order:
        topic: orders-exchange
        batch-size: 500
        polling:
          min-fixed-delay: 1s
        max-retries: 2
        backoff:
          delay: 5s
          multiplier: 2.0

      update-order:
        topic: orders-exchange
        batch-size: 200
        polling:
          type: fixed
          initial-delay: 10m
          fixed-delay: 250ms
        backoff:
          enabled: false  # Use fixed delay instead
      
      delete-order:
        topic: orders-exchange
        batch-size: 500
        polling:
          max-fixed-delay: 5m
          multiplier: 5.0
```

**Example with inheritance from `defaults`:**
```yaml
oncebox:
  publisher:
    defaults:
      batch-size: 200
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 1m
        multiplier: 1.5
      max-retries: 3
      backoff:
        enabled: true
        delay: 10s
        multiplier: 3.0
    
    events:
      high-priority:
        topic: events
        polling:
          min-fixed-delay: 250ms  # Override: faster polling
        # Inherits: batch-size=200, other polling settings, max-retries=3, backoff 
      
      low-priority:
        topic: events
        polling:
          min-fixed-delay: 10s  # Override: slower polling
          max-fixed-delay: 5m
          multiplier: 2.0
        backoff:
          enabled: false  # Override: disable backoff
          # Inherits: batch-size=200, other polling settings, max-retries=3
```
---

#### Stuck Event Recovery
```yaml
oncebox:
  publisher:
    stuck-recovery:
      batch-size: 500
      max-batch-processing-time: 300s
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 1m
        multiplier: 4.0
```

| Property                    | Description                                                                                                          |  Default   |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------|:----------:|
| `batch-size`                | Number of stuck events to recover per iteration                                                                      |   `500`    |
| `max-batch-processing-time` | Time threshold for detecting stuck events (events in `IN_PROCESS` longer than this are considered stuck)             |   `300s`   |
| `polling.type`              | Polling type (`fixed` or `adaptive`)                                                                                 | `adaptive` |
| `polling.initial-delay`     | Delay before first polling starts                                                                                    |    `5m`    |
| `polling.min-fixed-delay`   | Min delay between polling iterations                                                                                 |    `1s`    |
| `polling.max-fixed-delay`   | Max delay between polling iterations                                                                                 |    `1m`    |
| `polling.multiplier`        | Multiplier for exponential backoff between polling iterations                                                        |   `4.0`    |
---

#### Cleanup
```yaml
oncebox:
  publisher:
    clean-up:
      enabled: true
      batch-size: 500
      ttl: 24h
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 5s
        max-fixed-delay: 1m
        multiplier: 2.0
```

> [!WARNING]
> When disabled, processed events will accumulate indefinitely.

| Property                  | Description                                                                                                          |  Default   |
|---------------------------|----------------------------------------------------------------------------------------------------------------------|:----------:|
| `enabled`                 | Enable automatic cleanup of processed events                                                                         |   `true`   |
| `batch-size`              | Number of events to delete per iteration                                                                             |   `500`    |
| `ttl`                     | TTL for processed events. Events with `PROCESSED` status older than this are deleted                                 |   `24h`    |
| `polling.type`            | Polling type (`fixed` or `adaptive`)                                                                                 | `adaptive` |
| `polling.initial-delay`   | Delay before first polling starts                                                                                    |    `5m`    |
| `polling.min-fixed-delay` | Min delay between polling iterations                                                                                 |    `5s`    |
| `polling.max-fixed-delay` | Max delay between polling iterations                                                                                 |    `1m`    |
| `polling.multiplier`      | Multiplier for exponential backoff between polling iterations                                                        |   `2.0`    |

---

#### Dead Letter Queue
```yaml
oncebox:
  publisher:
    dlq:
      enabled: true  
      batch-size: 500
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 2m
        multiplier: 10.0
      transfer-to:
        # All properties inherit from section above
      transfer-from:
        # All properties inherit from section above
      clean-up:
        enabled: true
        batch-size: 500
        ttl: 24h
        polling:
          type: adaptive
          initial-delay: 5m
          min-fixed-delay: 5s
          max-fixed-delay: 1m
          multiplier: 2.0
```
DLQ has a shared section with `batch-size`, `polling`. Values from this section will be used in `transfer-to` and `transfer-from` as defaults.
> [!WARNING]
> When disabled, failed events are not managed automatically and stay in `outbox_events` as `FAILED`.

| Property                  | Description                                                                                                            |  Default   |
|---------------------------|------------------------------------------------------------------------------------------------------------------------|:----------:|
| `enabled`                 | Enable DLQ functionality                                                                                               |  `false`   |
| `batch-size`              | Number of events to transfer per iteration                                                                             |   `500`    |
| `polling.type`            | Polling type (`fixed` or `adaptive`)                                                                                   | `adaptive` |
| `polling.initial-delay`   | Delay before first polling starts                                                                                      |    `5m`    |
| `polling.min-fixed-delay` | Min delay between polling iterations                                                                                   |    `1s`    |
| `polling.max-fixed-delay` | Max delay between polling iterations                                                                                   |    `2m`    |
| `polling.multiplier`      | Multiplier for exponential backoff between polling iterations                                                          |   `10.0`   |
| `transfer-to`             | Section for specifying settings for transferring events from the outbox to the DLQ                                     |     —      |
| `transfer-from`           | Section for specifying settings for transferring events from the DLQ to the outbox                                     |     —      |
| `clean-up`                | Specifying cleanup settings for DLQ events with `RESOLVED` status, parameters and defaults same as [here](#cleanup-3). |     —      |

**Override example**: 
```yaml
oncebox:
  publisher:
    dlq:
      enabled: true  
      batch-size: 500
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 2m
        multiplier: 10.0
      transfer-to:
        polling:
          multiplier: 2.5
        # Inherits: batch-size=500, other polling settings except multiplier
      transfer-from:
        batch-size: 1000
        polling:
          type: fixed
          initial-delay: 5m
          fixed-delay: 1m
        # Nothing inherits
```
---

#### Metrics
```yaml
oncebox:
  publisher:
    metrics:
      enabled: true
      gauge:
        enabled: true
        cache:
          ttls: [60s, 60s, 30s]
```
| Property              | Description                                        |       Default       |
|-----------------------|----------------------------------------------------|:-------------------:|
| `metrics.enabled`     | Enable metrics collection                          |       `false`       |
| `gauge.enabled`       | Enable gauge metrics collection                    |       `false`       |
| `gauge.cache.enabled` | Enable cache (false when `gauge.enabled` is false) |       `false`       |
| `gauge.cache.ttls`    | TTL for caching different gauge metric values      |  `[60s, 60s, 60s]`  |

This enables metrics collection only, gauges stay disabled until `gauge.enabled` is set to `true`:
```yaml
oncebox:
  publisher:
    metrics:
      enabled: true
```
---

### Consumer

#### Source
```yaml
oncebox:
  consumer:
    source:
      type: kafka
```

| Property            | Description                                  |
|---------------------|----------------------------------------------|
| `type`              | Message broker type (`kafka` or `rabbit`)    |

---

#### Mappings

```yaml
oncebox:
  consumer:
    mappings:
      create-order: "com.example.OrderCreatedDto"
      update-order: "com.example.OrderUpdatedDto"
      delete-order: "com.example.OrderDeletedDto"
```

| Property   | Description                                                                                                                                                                                                                                                                                          |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mappings` | A map where keys are event types and values are the target event object classes. The keys in this map must exactly match the event types configured on the publisher side under `oncebox.publisher.events`. For example: `create-order`, `update-order`, `delete-order`. |

---

#### Cleanup
```yaml
oncebox:
  consumer:
    clean-up:
      enabled: true
      batch-size: 500
      ttl: 24h
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 5s
        max-fixed-delay: 1m
        multiplier: 2.0
```
All parameters and defaults same as [here](#cleanup-3).

---

#### Cache
The library uses Spring's `CacheManager` from application context.
```yaml
oncebox:
  consumer:
    cache:
      enabled: true
      cache-name: "<service-name>:oncebox:consumed"
```

> [!WARNING]
> When disabled, idempotency check always hits database.

| Property     | Description                                                                                                                                           |  Default  |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|:---------:|
| `enabled`    | Enable distributed caching of consumed event ids. Disabled when the `cache` section is absent, enabled when it is present and `enabled` is not set      |  `false`  |
| `cache-name` | Name of the cache in CacheManager (**required** when `consumer.cache.enabled` is true). Must match cache name configured in your `CacheManager` bean. Must be unique for each service sharing a cache store, see [Idempotent Processing](#idempotent-processing). |     —     |

---

#### Metrics
```yaml
oncebox:
  consumer:
    metrics:
      enabled: true
```

| Property          | Description               |  Default  |
|-------------------|---------------------------|:---------:|
| `metrics.enabled` | Enable metrics collection |  `false`  |

---

### Examples
#### Publisher-Only
Minimal:
> [!WARNING]
> Dead Letter Queue and metrics collection are disabled by default. All other values will use defaults.

```yaml
oncebox:
  publisher:
    sender:
      type: kafka
    events:
      # Your events with specific polling, retry, backoff values
```
Minimal with all features:

```yaml
oncebox:
  publisher:
    sender:
      type: kafka
    events:
      # Your events with specific polling, retry, backoff values 
    dlq:
      enabled: true
    metrics:
      enabled: true
      gauge:
        enabled: true
```

Full:
```yaml
oncebox:
  thread-pool-size: 5
  tables:
    auto-create: true
    
  publisher:
    sender:
      type: kafka
      emergency-timeout: 120s

    defaults:
      batch-size: 200
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 250ms
        max-fixed-delay: 1m
        multiplier: 1.5
      max-retries: 3
      backoff:
        enabled: true
        delay: 10s
        multiplier: 3.0
    
    events:
      # Your events with specific polling, retry, backoff values 
    
    stuck-recovery:
      batch-size: 500
      max-batch-processing-time: 300s
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 1m
        multiplier: 4.0
    
    clean-up:
      enabled: true
      batch-size: 200
      ttl: 24h
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 500ms
        max-fixed-delay: 1m
        multiplier: 2.0
    
    dlq:
      enabled: true
      batch-size: 500
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 1s
        max-fixed-delay: 2m
        multiplier: 10.0
      transfer-to:
        # Specific to DLQ polling settings if necessary
      transfer-from:
        # Specific from DLQ polling settings if necessary
    metrics:
      enabled: true
      gauge:          
        enabled: true
        cache:
          ttls: [60s, 60s, 60s]
```

---

#### Consumer-Only
Minimal:

> [!WARNING]
> Cleanup is enabled by default, cache is enabled once `cache.cache-name` is set, metrics are disabled.

```yaml
oncebox:
  publisher:
    enabled: false
  consumer:
    enabled: true
    source:
      type: kafka
    mappings:
      # Your specific mappings
    cache:
      cache-name: "<service-name>:oncebox:consumed"
```

Minimal with all features:
```yaml
oncebox:
  publisher:
    enabled: false
  consumer:
    enabled: true
    source:
      type: kafka
    mappings:
      # Your specific mappings
    cache:
      cache-name: "<service-name>:oncebox:consumed"
    metrics:
      enabled: true
```

Full:
```yaml
oncebox:
  thread-pool-size: 2
  tables:
    auto-create: true

  publisher:
    enabled: false

  consumer:
    enabled: true
    source:
      type: kafka
    mappings:
      # Your specific mappings    
    clean-up:
      enabled: true
      batch-size: 200
      ttl: 24h
      polling:
        type: adaptive
        initial-delay: 5m
        min-fixed-delay: 500ms
        max-fixed-delay: 1m
        multiplier: 2.0
    cache:
      enabled: true
      cache-name: "<service-name>:oncebox:consumed"
    metrics:
      enabled: true
```