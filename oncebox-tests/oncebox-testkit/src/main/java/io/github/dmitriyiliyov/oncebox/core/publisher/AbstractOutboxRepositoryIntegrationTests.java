package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

public class AbstractOutboxRepositoryIntegrationTests {

    private static final UUID LOCK_TOKEN = UUID.fromString("0192f5a0-0000-7000-8000-0000000000b1");
    private static final UUID OTHER_LOCK_TOKEN = UUID.fromString("0192f5a0-0000-7000-8000-0000000000b2");

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
        Set<UUID> ids = capture(LOCK_TOKEN, 2);

        int updated = repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PENDING);

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_toProcessed_updatesAll() {
        Set<UUID> ids = capture(LOCK_TOKEN, 2);

        int updated = repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PROCESSED);

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_toFailed_throwsException() {
        Set<UUID> ids = capture(LOCK_TOKEN, 1);

        assertThatThrownBy(() ->
                repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.FAILED)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("partiallyUpdateBatchByLockToken");
    }

    public void updateBatchStatus_emptyIds_returnsZero() {
        assertThat(repository.updateBatchStatusByLockToken(Set.of(), LOCK_TOKEN, EventStatus.PROCESSED))
                .isEqualTo(0);
    }

    public void updateBatchStatus_doesNotAffectOtherEvents() {
        Set<UUID> target = capture(LOCK_TOKEN, 1);
        OutboxEvent unrelated = buildEventWithNextRetryAt(EventStatus.PENDING, Instant.now().minusSeconds(1));
        repository.saveBatch(List.of(unrelated));

        repository.updateBatchStatusByLockToken(target, LOCK_TOKEN, EventStatus.PROCESSED);

        List<OutboxEvent> stillPending = repository.findAndLockBatchByStatus(
                EventStatus.PENDING, 10, EventStatus.IN_PROCESS
        );
        assertThat(stillPending)
                .extracting(OutboxEvent::getId)
                .contains(unrelated.getId());
    }

    public void updateBatchStatus_whenHeldByAnotherLockToken_updatesNothing() {
        Set<UUID> ids = capture(LOCK_TOKEN, 2);

        int stale = repository.updateBatchStatusByLockToken(ids, OTHER_LOCK_TOKEN, EventStatus.PROCESSED);

        assertThat(stale).isZero();
        assertThat(repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PROCESSED)).isEqualTo(2);
    }

    public void updateBatchStatus_whenNeverCaptured_updatesNothing() {
        OutboxEvent event = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(event));

        int updated = repository.updateBatchStatusByLockToken(Set.of(event.getId()), LOCK_TOKEN, EventStatus.PROCESSED);

        assertThat(updated).isZero();
    }

    public void partiallyUpdateBatch_incrementsRetryCount() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();

        OutboxEvent updated = buildEventWithRetry(
                id, EventStatus.FAILED, 1,
                Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS)
        );
        int count = repository.partiallyUpdateBatchByLockToken(List.of(updated), LOCK_TOKEN);

        assertThat(count).isEqualTo(1);
    }

    public void partiallyUpdateBatch_emptyList_returnsZero() {
        assertThat(repository.partiallyUpdateBatchByLockToken(List.of(), LOCK_TOKEN)).isEqualTo(0);
    }

    public void partiallyUpdateBatch_nullList_returnsZero() {
        assertThat(repository.partiallyUpdateBatchByLockToken(null, LOCK_TOKEN)).isEqualTo(0);
    }

    public void partiallyUpdateBatch_multipleEvents_allUpdated() {
        List<UUID> ids = List.copyOf(capture(LOCK_TOKEN, 2));

        Instant nextRetry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        int count = repository.partiallyUpdateBatchByLockToken(List.of(
                buildEventWithRetry(ids.get(0), EventStatus.FAILED, 1, nextRetry),
                buildEventWithRetry(ids.get(1), EventStatus.FAILED, 2, nextRetry)
        ), LOCK_TOKEN);

        assertThat(count).isEqualTo(2);
    }

    public void partiallyUpdateBatch_whenHeldByAnotherLockToken_updatesNothing() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        Instant nextRetry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        List<OutboxEvent> outcome = List.of(buildEventWithRetry(id, EventStatus.FAILED, 1, nextRetry));

        int stale = repository.partiallyUpdateBatchByLockToken(outcome, OTHER_LOCK_TOKEN);

        assertThat(stale).isZero();
        assertThat(repository.partiallyUpdateBatchByLockToken(outcome, LOCK_TOKEN)).isEqualTo(1);
    }

    public void findAndLockBatchByStatus_shouldKeepLockTokenOfLastCapture() {
        Set<UUID> ids = capture(LOCK_TOKEN, 1);
        repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PENDING);

        List<OutboxEvent> relocked = repository.findAndLockBatchByStatus(EventStatus.PENDING, 10, EventStatus.IN_PROCESS);

        assertThat(relocked).extracting(OutboxEvent::getId).containsAll(ids);
        assertThat(repository.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PROCESSED)).isEqualTo(1);
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

    /**
     * Saves {@code count} due events of an event type of their own and captures them with the given token, the
     * way a poller does - the finalization methods only touch events captured with the token they are given.
     */
    private Set<UUID> capture(UUID lockToken, int count) {
        String eventType = "capture-" + UUID.randomUUID();
        Instant due = Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        List<OutboxEvent> events = IntStream.range(0, count)
                .mapToObj(i -> buildEventWithTypeAndNextRetryAt(EventStatus.PENDING, eventType, due))
                .toList();
        repository.saveBatch(events);
        List<OutboxEvent> captured = repository.findAndLockBatchByEventTypeAndStatus(
                eventType, EventStatus.PENDING, count, lockToken, EventStatus.IN_PROCESS
        );
        assertThat(captured).hasSize(count);
        return captured.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
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
