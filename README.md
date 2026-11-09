[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/oncebox)
[![codecov](https://codecov.io/github/dmitriy-iliyov/oncebox/branch/main/graph/badge.svg?token=8X6B9K3AOK)](https://codecov.io/github/dmitriy-iliyov/oncebox)
[![CI](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/ci.yml)
[![E2E](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/e2e.yml/badge.svg)](https://github.com/dmitriy-iliyov/oncebox/actions/workflows/e2e.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/oncebox-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/oncebox-starter)
[![javadoc](https://javadoc.io/badge2/io.github.dmitriy-iliyov/oncebox-core/javadoc.svg)](https://javadoc.io/doc/io.github.dmitriy-iliyov/oncebox-core)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/oncebox)
[![GitHub Release Date](https://img.shields.io/github/release-date/dmitriy-iliyov/oncebox)](https://github.com/dmitriy-iliyov/oncebox/releases/latest)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/oncebox)

<br>

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/images/oncebox-lockup-dark.svg">
    <img src="docs/images/oncebox-lockup.svg" alt="oncebox - Transactional Outbox and Inbox for Spring Boot" height="125">
  </picture>
</p>

## Overview
This library provides an implementation of the [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html) based on polling events from a relational database and publishing them to a message broker.

The library offers flexible configuration for processing different event types.
Each event type is handled independently, and events are processed in parallel using a configurable thread pool, allowing predictable throughput and controlled resource usage.

This approach ensures reliable event publication without relying on database log-based CDC solutions, making it suitable for environments where simplicity, portability, and explicit control over event processing are preferred.

## Documentation

The full documentation lives **[here](https://dmitriy-iliyov.github.io/oncebox-docs/)**:

- [Quickstart](https://dmitriy-iliyov.github.io/oncebox-docs/docs/quickstart/) - add the dependencies, configure an event type, publish and consume it.
- [Delivery Guarantees](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/guarantees/) - what is guaranteed, in which failure, and what is not.
- [Configuration](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/configuration/) - every property with its default.

## Key Features
- **Atomic persistence** - stores events within the same business transaction, ensuring consistency.
- **Adaptive polling** - dynamically adjusts the polling interval based on workload to optimize database usage.
- **Flexible polling configuration** - allows customization of polling intervals, retry policies, and backoff strategies per event type.
- **At-least-once processing** - guarantees outbox delivery with configurable retries and backoff policies.
- **Effectively-once processing** - optionally records an incoming event in the inbox within the same transaction as its handling, so a redelivered event never changes data twice.
- **At-most-once processing** - optionally records an incoming event in the inbox before handling it, so a side effect such as sending an email is never repeated.
- **Dead Letter Queue** - stores events that fail after all retry attempts, with REST API support for management.
- **Background service** - handles recovery of stuck events and cleanup of processed or consumed events automatically.
- **Observability** - provides out-of-the-box metrics integration via Micrometer.

## Supported Infrastructure

- **Databases:** PostgreSQL 16+, MySQL 8.0.13+, Oracle 23+.
- **Message Brokers:** Apache Kafka 3.7+, RabbitMQ 3.12+.
- **Cache Storage (optional for caching):** Redis 7+.

## Limitations
- **Horizontal scaling** - performance degradation may occur when reaching a certain number of instances due to `SKIP LOCKED` - based concurrent polling mechanism. The optimal number of instances depends on the number of event types and database load.
- **No ordering guarantees** - events are processed in parallel by type, with no guaranteed delivery order within or across event types.
