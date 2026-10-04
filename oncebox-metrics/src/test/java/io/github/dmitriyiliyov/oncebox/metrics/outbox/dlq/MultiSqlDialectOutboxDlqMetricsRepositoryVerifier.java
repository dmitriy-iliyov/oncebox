package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MultiSqlDialectOutboxDlqMetricsRepositoryVerifier {

    private final MultiDialectOutboxDlqMetricsRepository repository;
    private final JdbcTemplate jdbcTemplate;

    MultiSqlDialectOutboxDlqMetricsRepositoryVerifier(
            MultiDialectOutboxDlqMetricsRepository repository,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM outbox_dlq_events");
    }

    void count_noEvents_returnsZero() {
        assertThat(repository.count()).isEqualTo(0L);
    }

    void count_withEvents_returnsTotal() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.RESOLVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.TO_RETRY, "PAYMENT_CREATED");

        assertThat(repository.count()).isEqualTo(3L);
    }

    void countByStatus_noEvents_returnsZero() {
        assertThat(repository.countByStatus(DlqStatus.MOVED)).isEqualTo(0L);
    }

    void countByStatus_returnsOnlyMatchingStatus() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.RESOLVED, "ORDER_CREATED");

        assertThat(repository.countByStatus(DlqStatus.MOVED)).isEqualTo(2L);
        assertThat(repository.countByStatus(DlqStatus.RESOLVED)).isEqualTo(1L);
        assertThat(repository.countByStatus(DlqStatus.TO_RETRY)).isEqualTo(0L);
    }

    void countByStatus_doesNotCountOtherStatuses() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");

        assertThat(repository.countByStatus(DlqStatus.RESOLVED)).isEqualTo(0L);
    }

    void countByEventTypeAndStatus_noEvents_returnsZero() {
        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED))
                .isEqualTo(0L);
    }

    void countByEventTypeAndStatus_returnsOnlyMatchingTypeAndStatus() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");
        insertDlqEvent(DlqStatus.MOVED, "PAYMENT_CREATED");
        insertDlqEvent(DlqStatus.RESOLVED, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED))
                .isEqualTo(2L);
    }

    void countByEventTypeAndStatus_wrongEventType_returnsZero() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("PAYMENT_CREATED", DlqStatus.MOVED))
                .isEqualTo(0L);
    }

    void countByEventTypeAndStatus_wrongStatus_returnsZero() {
        insertDlqEvent(DlqStatus.MOVED, "ORDER_CREATED");

        assertThat(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.RESOLVED))
                .isEqualTo(0L);
    }

    protected Object idParam(UUID id) {
        return id;
    }

    private void insertDlqEvent(DlqStatus dlqStatus, String eventType) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("""
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                idParam(UUID.randomUUID()), "FAILED", dlqStatus.name(), eventType,
                "io.example.OrderCreated", "{}", 0,
                Timestamp.from(now.plusSeconds(60)),
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now)
        );
    }
}