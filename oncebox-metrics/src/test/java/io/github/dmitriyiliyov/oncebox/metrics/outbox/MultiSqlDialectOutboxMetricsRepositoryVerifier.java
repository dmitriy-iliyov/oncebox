package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MultiSqlDialectOutboxMetricsRepositoryVerifier {

    private final MultiDialectOutboxMetricsRepository repository;
    protected final JdbcTemplate jdbcTemplate;

    MultiSqlDialectOutboxMetricsRepositoryVerifier(
            MultiDialectOutboxMetricsRepository repository,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM outbox_events");
    }

    void count_noEvents_returnsZero() {
        assertThat(repository.count()).isEqualTo(0L);
    }

    void count_withEvents_returnsTotal() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");
        insertEvent(EventStatus.IN_PROCESS, "ORDER_CREATED");
        insertEvent(EventStatus.PROCESSED, "PAYMENT_CREATED");

        assertThat(repository.count()).isEqualTo(3L);
    }

    void countByStatus_noEvents_returnsZero() {
        assertThat(repository.countByStatus(EventStatus.PENDING)).isEqualTo(0L);
    }

    void countByStatus_returnsOnlyMatchingStatus() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");
        insertEvent(EventStatus.PROCESSED, "ORDER_CREATED");

        assertThat(repository.countByStatus(EventStatus.PENDING)).isEqualTo(2L);
        assertThat(repository.countByStatus(EventStatus.PROCESSED)).isEqualTo(1L);
        assertThat(repository.countByStatus(EventStatus.IN_PROCESS)).isEqualTo(0L);
    }

    void countByStatus_doesNotCountOtherStatuses() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");

        assertThat(repository.countByStatus(EventStatus.PROCESSED)).isEqualTo(0L);
    }

    void countByEventTypeAndStatus_noEvents_returnsZero() {
        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING))
                .isEqualTo(0L);
    }

    void countByEventTypeAndStatus_returnsOnlyMatchingTypeAndStatus() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");
        insertEvent(EventStatus.PENDING, "PAYMENT_CREATED");
        insertEvent(EventStatus.PROCESSED, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING))
                .isEqualTo(2L);
    }

    void countByEventTypeAndStatus_wrongEventType_returnsZero() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("PAYMENT_CREATED", EventStatus.PENDING))
                .isEqualTo(0L);
    }

    void countByEventTypeAndStatus_wrongStatus_returnsZero() {
        insertEvent(EventStatus.PENDING, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PROCESSED))
                .isEqualTo(0L);
    }

    protected Object idParam(UUID id) {
        return id;
    }

    private void insertEvent(EventStatus status, String eventType) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("""
            INSERT INTO outbox_events
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                idParam(UUID.randomUUID()), status.name(), eventType,
                "io.example.OrderCreated", "{}", 0,
                Timestamp.from(now.plusSeconds(60)),
                Timestamp.from(now), Timestamp.from(now)
        );
    }
}