package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

public class AbstractOutboxRepositoryIntegrationTests {

    private final OutboxRepository repository;

    public AbstractOutboxRepositoryIntegrationTests(OutboxRepository repository) {
        this.repository = repository;
    }

    public void save_singleEvent_persistedCorrectly() {
        OutboxEvent event = buildEvent(EventStatus.PENDING);

        repository.save(event);

        OutboxEvent found = repository.findAndLockBatchByStatus(
                        EventStatus.PENDING, 10, EventStatus.IN_PROCESS
                ).stream()
                .filter(e -> e.getId().equals(event.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(event.getId());
        assertThat(found.getStatus()).isEqualTo(EventStatus.IN_PROCESS);
        assertThat(found.getEventType()).isEqualTo(event.getEventType());
        assertThat(found.getPayloadType()).isEqualTo(event.getPayloadType());
        assertThat(found.getPayload()).isEqualTo(event.getPayload());
        assertThat(found.getRetryCount()).isEqualTo(event.getRetryCount());
    }

    public void saveBatch_multipleEvents_allPersisted() {
        List<OutboxEvent> events = List.of(
                buildEvent(EventStatus.PENDING),
                buildEvent(EventStatus.PENDING),
                buildEvent(EventStatus.PENDING)
        );

        repository.saveBatch(events);

        List<OutboxEvent> found = repository.findAndLockBatchByStatus(
                EventStatus.PENDING, 10, EventStatus.IN_PROCESS
        );
        List<UUID> foundIds = found.stream().map(OutboxEvent::getId).toList();

        assertThat(foundIds).containsAll(
                events.stream().map(OutboxEvent::getId).toList()
        );
    }

    public void saveBatch_emptyList_doesNotThrow() {
        assertThatCode(() -> repository.saveBatch(List.of()))
                .doesNotThrowAnyException();
    }

    public void updateBatchStatus_toPending_updatesAll() {
        OutboxEvent e1 = buildEvent(EventStatus.IN_PROCESS);
        OutboxEvent e2 = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(e1, e2));

        int updated = repository.updateBatchStatus(
                Set.of(e1.getId(), e2.getId()), EventStatus.PENDING
        );

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_toProcessed_updatesAll() {
        OutboxEvent e1 = buildEvent(EventStatus.IN_PROCESS);
        OutboxEvent e2 = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(e1, e2));

        int updated = repository.updateBatchStatus(
                Set.of(e1.getId(), e2.getId()), EventStatus.PROCESSED
        );

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_toFailed_throwsException() {
        OutboxEvent event = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(event));

        assertThatThrownBy(() ->
                repository.updateBatchStatus(Set.of(event.getId()), EventStatus.FAILED)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("partiallyUpdateBatch");
    }

    public void updateBatchStatus_emptyIds_returnsZero() {
        assertThat(repository.updateBatchStatus(Set.of(), EventStatus.PROCESSED))
                .isEqualTo(0);
    }

    public void updateBatchStatus_doesNotAffectOtherEvents() {
        OutboxEvent target    = buildEvent(EventStatus.IN_PROCESS);
        OutboxEvent unrelated = buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(target, unrelated));

        repository.updateBatchStatus(Set.of(target.getId()), EventStatus.PROCESSED);

        List<OutboxEvent> stillPending = repository.findAndLockBatchByStatus(
                EventStatus.PENDING, 10, EventStatus.IN_PROCESS
        );
        assertThat(stillPending)
                .extracting(OutboxEvent::getId)
                .contains(unrelated.getId());
    }

    public void partiallyUpdateBatch_incrementsRetryCount() {
        OutboxEvent event = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(event));

        OutboxEvent updated = buildEventWithRetry(
                event.getId(), EventStatus.FAILED, 1,
                Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS)
        );
        int count = repository.partiallyUpdateBatch(List.of(updated));

        assertThat(count).isEqualTo(1);
    }

    public void partiallyUpdateBatch_emptyList_returnsZero() {
        assertThat(repository.partiallyUpdateBatch(List.of())).isEqualTo(0);
    }

    public void partiallyUpdateBatch_nullList_returnsZero() {
        assertThat(repository.partiallyUpdateBatch(null)).isEqualTo(0);
    }

    public void partiallyUpdateBatch_multipleEvents_allUpdated() {
        OutboxEvent e1 = buildEvent(EventStatus.IN_PROCESS);
        OutboxEvent e2 = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(e1, e2));

        Instant nextRetry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        int count = repository.partiallyUpdateBatch(List.of(
                buildEventWithRetry(e1.getId(), EventStatus.FAILED, 1, nextRetry),
                buildEventWithRetry(e2.getId(), EventStatus.FAILED, 2, nextRetry)
        ));

        assertThat(count).isEqualTo(2);
    }

    public void deleteBatch_existingIds_deletedAndReturnsCount() {
        OutboxEvent e1 = buildEvent(EventStatus.PENDING);
        OutboxEvent e2 = buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(e1, e2));

        int deleted = repository.deleteBatch(Set.of(e1.getId(), e2.getId()));

        assertThat(deleted).isEqualTo(2);
    }

    public void deleteBatch_emptyIds_returnsZero() {
        assertThat(repository.deleteBatch(Set.of())).isEqualTo(0);
    }

    public void deleteBatch_doesNotAffectOtherEvents() {
        OutboxEvent target    = buildEvent(EventStatus.PENDING);
        OutboxEvent unrelated = buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(target, unrelated));

        repository.deleteBatch(Set.of(target.getId()));

        List<OutboxEvent> remaining = repository.findAndLockBatchByStatus(
                EventStatus.PENDING, 10, EventStatus.IN_PROCESS
        );
        assertThat(remaining)
                .extracting(OutboxEvent::getId)
                .contains(unrelated.getId())
                .doesNotContain(target.getId());
    }

    public void deleteBatch_notExistingIds_returnsZero() {
        assertThat(repository.deleteBatch(
                Set.of(UUID.randomUUID(), UUID.randomUUID()))
        ).isEqualTo(0);
    }

    public OutboxEvent buildEvent(EventStatus status) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEvent(
                UUID.randomUUID(),
                status,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                -1,
                now.plusSeconds(60),
                now,
                now
        );
    }

    private OutboxEvent buildEventWithRetry(
            UUID id, EventStatus status, int retryCount, Instant nextRetryAt
    ) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEvent(
                id,
                status,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                retryCount,
                nextRetryAt,
                now,
                now
        );
    }

    public OutboxEvent buildEventWithType(EventStatus status, String eventType) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEvent(
                UUID.randomUUID(), status, eventType,
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                -1, now.plusSeconds(60), now, now
        );
    }

    public OutboxEvent buildEventWithNextRetryAt(EventStatus status, Instant nextRetryAt) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEvent(
                UUID.randomUUID(), status, "ORDER_CREATED",
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                -1, nextRetryAt, now, now
        );
    }

    public OutboxEvent buildEventWithTypeAndNextRetryAt(EventStatus status, String eventType, Instant nextRetryAt) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEvent(
                UUID.randomUUID(), status, eventType,
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                -1, nextRetryAt, now, now
        );
    }
}
