package io.github.dmitriyiliyov.oncebox.core.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOutboxDlqTransferVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final OutboxRepository outboxRepository;
    private final OutboxDlqRepository dlqRepository;
    private final DefaultOutboxDlqTransfer transfer;
    private final IdExtractor idExtractor;

    @FunctionalInterface
    public interface IdExtractor {
        UUID extract(Object raw);
    }

    public DefaultOutboxDlqTransferVerifier(
            JdbcTemplate jdbcTemplate,
            OutboxRepository outboxRepository,
            OutboxDlqRepository dlqRepository,
            DefaultOutboxDlqTransfer transfer,
            IdExtractor idExtractor
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxRepository = outboxRepository;
        this.dlqRepository = dlqRepository;
        this.idExtractor = idExtractor;
        this.transfer = transfer;
    }

    public void transferToDlq_failedEvents_movedToDlqAndDeletedFromOutbox() {
        OutboxEvent failed1 = saveOutboxEvent(EventStatus.FAILED);
        OutboxEvent failed2 = saveOutboxEvent(EventStatus.FAILED);
        OutboxEvent pending = saveOutboxEvent(EventStatus.PENDING);

        transfer.transferToDlq(10);

        List<UUID> dlqIds = queryDlqIdsByStatus("MOVED");
        assertThat(dlqIds)
                .containsExactlyInAnyOrder(failed1.getId(), failed2.getId())
                .doesNotContain(pending.getId());

        List<UUID> outboxIds = queryOutboxIdsByStatus("PENDING");
        assertThat(outboxIds).containsOnly(pending.getId());

        List<UUID> deletedIds = queryOutboxIdsByStatus("FAILED");
        assertThat(deletedIds).isEmpty();
    }

    public void transferToDlq_noFailedEvents_doesNothing() {
        saveOutboxEvent(EventStatus.PENDING);

        transfer.transferToDlq(10);

        assertThat(queryDlqIdsByStatus("MOVED")).isEmpty();
    }

    public void transferToDlq_respectsBatchSize() {
        saveOutboxEvent(EventStatus.FAILED);
        saveOutboxEvent(EventStatus.FAILED);
        saveOutboxEvent(EventStatus.FAILED);

        transfer.transferToDlq(2);

        assertThat(queryDlqIdsByStatus("MOVED")).hasSize(2);
        assertThat(queryOutboxIdsByStatus("FAILED")).hasSize(1);
    }

    public void transferToDlq_preservesEventData() {
        OutboxEvent failed = saveOutboxEvent(EventStatus.FAILED);

        transfer.transferToDlq(10);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM outbox_dlq_events WHERE dlq_status = 'MOVED'"
        );
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("event_type")).isEqualTo(failed.getEventType());
        assertThat(row.get("payload")).isEqualTo(failed.getPayload());
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("dlq_status")).isEqualTo("MOVED");
    }

    public void transferFromDlq_toRetryEvents_movedBackToOutbox() {
        OutboxDlqEvent toRetry1 = saveDlqEvent(DlqStatus.TO_RETRY);
        OutboxDlqEvent toRetry2 = saveDlqEvent(DlqStatus.TO_RETRY);
        OutboxDlqEvent moved = saveDlqEvent(DlqStatus.MOVED);

        transfer.transferFromDlq(10);

        List<UUID> outboxIds = queryOutboxIdsByStatus("PENDING");
        assertThat(outboxIds)
                .containsExactlyInAnyOrder(toRetry1.getId(), toRetry2.getId())
                .doesNotContain(moved.getId());

        assertThat(countDlqByStatus("TO_RETRY")).isEqualTo(0);
        assertThat(countDlqByStatus("MOVED")).isEqualTo(1);
    }

    public void transferFromDlq_noToRetryEvents_doesNothing() {
        saveDlqEvent(DlqStatus.MOVED);

        transfer.transferFromDlq(10);

        assertThat(queryOutboxIdsByStatus("PENDING")).isEmpty();
        assertThat(countDlqByStatus("MOVED")).isEqualTo(1);
    }

    public void transferFromDlq_respectsBatchSize() {
        saveDlqEvent(DlqStatus.TO_RETRY);
        saveDlqEvent(DlqStatus.TO_RETRY);
        saveDlqEvent(DlqStatus.TO_RETRY);

        transfer.transferFromDlq(2);

        assertThat(queryOutboxIdsByStatus("PENDING")).hasSize(2);
        assertThat(countDlqByStatus("TO_RETRY")).isEqualTo(1);
    }

    public void transferFromDlq_resetsRetryCountToZero() {
        saveDlqEventWithRetryCount(DlqStatus.TO_RETRY, 3);

        transfer.transferFromDlq(10);

        Integer retryCount = jdbcTemplate.queryForObject(
                "SELECT retry_count FROM outbox_events WHERE status = 'PENDING'",
                Integer.class
        );
        assertThat(retryCount).isEqualTo(-1);
    }

    public void transferFromDlq_preservesEventData() {
        OutboxDlqEvent toRetry = saveDlqEvent(DlqStatus.TO_RETRY);

        transfer.transferFromDlq(10);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM outbox_events WHERE status = 'PENDING'"
        );
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("event_type")).isEqualTo(toRetry.getEventType());
        assertThat(row.get("payload")).isEqualTo(toRetry.getPayload());
    }

    public void transferToDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        List<OutboxEvent> events = saveEventsWithRetryCount(eventCount, EventStatus.FAILED);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    transfer.transferToDlq(eventCount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.awaitTermination(1, TimeUnit.SECONDS);

        List<UUID> ids = queryDlqIdsByStatus("MOVED");
        assertThat(ids).hasSize(eventCount);
        assertThat(ids).containsAll(events.stream().map(OutboxEvent::getId).toList());
        assertThat(queryOutboxIdsByStatus("FAILED")).isEmpty();
    }

    public void transferFromDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        List<OutboxDlqEvent> events = saveDlqEventsWithRetryCount(eventCount, DlqStatus.TO_RETRY, 3);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    transfer.transferFromDlq(eventCount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.awaitTermination(1, TimeUnit.SECONDS);

        List<UUID> ids = queryOutboxIdsByStatus("PENDING");
        assertThat(ids).hasSize(eventCount);
        assertThat(ids).containsAll(events.stream().map(OutboxEvent::getId).toList());
        assertThat(queryDlqIdsByStatus("TO_RETRY")).isEmpty();
    }

    private List<UUID> queryOutboxIdsByStatus(String status) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM outbox_events WHERE status = ?", status
        ).stream().map(row -> idExtractor.extract(row.get("id"))).toList();
    }

    private List<UUID> queryDlqIdsByStatus(String dlqStatus) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM outbox_dlq_events WHERE dlq_status = ?", dlqStatus
        ).stream().map(row -> idExtractor.extract(row.get("id"))).toList();
    }

    private int countDlqByStatus(String dlqStatus) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?",
                Integer.class, dlqStatus
        );
        return count == null ? 0 : count;
    }

    private OutboxEvent saveOutboxEvent(EventStatus status) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), status, "ORDER_CREATED",
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                status == EventStatus.FAILED ? 3 : 0,
                now.plusSeconds(60), now, now
        );
        outboxRepository.saveBatch(List.of(event));
        return event;
    }

    private List<OutboxEvent> saveEventsWithRetryCount(int eventCount, EventStatus status) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<OutboxEvent> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            events.add(
                    new OutboxEvent(
                            UUID.randomUUID(), status, "ORDER_CREATED",
                            "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                            status == EventStatus.FAILED ? 3 : 0,
                            now.plusSeconds(60), now, now
                    )
            );
        }
        outboxRepository.saveBatch(events);
        return events;
    }

    private OutboxDlqEvent saveDlqEvent(DlqStatus dlqStatus) {
        return saveDlqEventWithRetryCount(dlqStatus, 3);
    }

    private OutboxDlqEvent saveDlqEventWithRetryCount(DlqStatus dlqStatus, int retryCount) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "ORDER_CREATED",
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                retryCount, now.plusSeconds(60), now, now, dlqStatus, now
        );
        dlqRepository.saveBatch(List.of(event));
        return event;
    }

    private List<OutboxDlqEvent> saveDlqEventsWithRetryCount(int eventCount, DlqStatus dlqStatus, int retryCount) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<OutboxDlqEvent> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            events.add(
                    new OutboxDlqEvent(
                            UUID.randomUUID(), EventStatus.FAILED, "ORDER_CREATED",
                            "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                            retryCount, now.plusSeconds(60), now, now, dlqStatus, now
                    )
            );
        }
        dlqRepository.saveBatch(events);
        return events;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}