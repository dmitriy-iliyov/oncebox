package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

public class AbstractOutboxRepositoryIntegrationTests {

    private static final UUID LOCK_TOKEN = UUID.fromString("0192f5a0-0000-7000-8000-0000000000b1");
    private static final UUID OTHER_LOCK_TOKEN = UUID.fromString("0192f5a0-0000-7000-8000-0000000000b2");
    private static final String TABLE = "outbox_events";
    private static final Duration AGE = Duration.ofMinutes(30);
    private static final Duration LONG_AGO = Duration.ofHours(1).negated();
    // the database time moves on between the statement that sets a time and the one that reads it back
    private static final Duration SLACK = Duration.ofSeconds(30);

    private final OutboxRepository repository;
    private final TestDatabaseClock clock;

    public AbstractOutboxRepositoryIntegrationTests(OutboxRepository repository, TestDatabaseClock clock) {
        this.repository = repository;
        this.clock = clock;
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

    public void save_whenEventIsNew_shouldLeaveEveryTimeToDatabase() {
        OutboxEvent event = buildEvent(EventStatus.PENDING);

        repository.save(event);

        assertThat(distance(event.getId(), "created_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(distance(event.getId(), "updated_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(distance(event.getId(), "next_retry_at")).isBetween(SLACK.negated(), SLACK);
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

    public void saveBatchWithCreatedAt_whenEventHasCreatedAt_shouldKeepItAndMakeEventDueAtOnce() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        String eventType = "restored-" + UUID.randomUUID();
        OutboxEvent event = buildEventWithCreatedAt(eventType, createdAt);

        repository.saveBatchWithCreatedAt(List.of(event));

        assertThat(distance(event.getId(), "next_retry_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(distance(event.getId(), "updated_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(repository.findAndLockBatchByEventTypeAndStatus(
                eventType, EventStatus.PENDING, 10, LOCK_TOKEN, EventStatus.IN_PROCESS
        )).singleElement().extracting(OutboxEvent::getCreatedAt).isEqualTo(createdAt);
    }

    public void saveBatchWithCreatedAt_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoment() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        String eventType = "restored-" + UUID.randomUUID();
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            repository.saveBatchWithCreatedAt(List.of(buildEventWithCreatedAt(eventType, createdAt)));
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));

            List<OutboxEvent> found = repository.findAndLockBatchByEventTypeAndStatus(
                    eventType, EventStatus.PENDING, 10, LOCK_TOKEN, EventStatus.IN_PROCESS
            );

            assertThat(found).singleElement().extracting(OutboxEvent::getCreatedAt).isEqualTo(createdAt);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    public void saveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowAndSaveNothing() {
        String eventType = "restored-" + UUID.randomUUID();
        OutboxEvent event = buildEventWithType(EventStatus.PENDING, eventType);

        assertThatThrownBy(() -> repository.saveBatchWithCreatedAt(List.of(event)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt cannot be null");
        assertThat(repository.findAndLockBatchByEventTypeAndStatus(
                eventType, EventStatus.PENDING, 10, LOCK_TOKEN, EventStatus.IN_PROCESS
        )).isEmpty();
    }

    public void saveBatchWithCreatedAt_whenBatchIsEmpty_shouldNotThrow() {
        assertThatCode(() -> repository.saveBatchWithCreatedAt(List.of()))
                .doesNotThrowAnyException();
    }

    public void findAndLockBatchByEventTypeAndStatus_whenNextRetryIsAhead_shouldSkipEvent() {
        String eventType = "retry-" + UUID.randomUUID();
        OutboxEvent due = buildEventWithType(EventStatus.PENDING, eventType);
        OutboxEvent notDue = buildEventWithType(EventStatus.PENDING, eventType);
        repository.saveBatch(List.of(due, notDue));
        clock.shift(TABLE, "next_retry_at", notDue.getId(), Duration.ofMinutes(5));

        List<OutboxEvent> locked = repository.findAndLockBatchByEventTypeAndStatus(
                eventType, EventStatus.PENDING, 10, LOCK_TOKEN, EventStatus.IN_PROCESS
        );

        assertThat(locked).extracting(OutboxEvent::getId).containsExactly(due.getId());
    }

    public void findAndLockBatchByEventTypeAndStatus_whenCaptured_shouldStampUpdatedAtByDatabaseClock() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();

        assertThat(distance(id, "updated_at")).isBetween(SLACK.negated(), SLACK);
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
        OutboxEvent unrelated = buildEvent(EventStatus.PENDING);
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

    public void updateBatchStatus_whenHeldByLockToken_shouldWriteStatusAndStampUpdatedAtByDatabaseClock() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        clock.shift(TABLE, "updated_at", id, LONG_AGO);

        repository.updateBatchStatusByLockToken(Set.of(id), LOCK_TOKEN, EventStatus.PROCESSED);

        assertThat(distance(id, "updated_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(idsWithStatus(EventStatus.PROCESSED)).contains(id);
    }

    public void updateBatchStatus_whenNeverCaptured_updatesNothing() {
        OutboxEvent event = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(event));

        int updated = repository.updateBatchStatusByLockToken(Set.of(event.getId()), LOCK_TOKEN, EventStatus.PROCESSED);

        assertThat(updated).isZero();
    }

    public void partiallyUpdateBatch_incrementsRetryCount() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();

        int count = repository.partiallyUpdateBatchByLockToken(
                List.of(buildEventWithRetry(id, EventStatus.FAILED, 1, null)), LOCK_TOKEN
        );

        assertThat(count).isEqualTo(1);
    }

    public void partiallyUpdateBatch_whenHeldByLockToken_shouldWriteRetryStateAndStampUpdatedAtByDatabaseClock() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        clock.shift(TABLE, "updated_at", id, LONG_AGO);

        repository.partiallyUpdateBatchByLockToken(
                List.of(buildEventWithRetry(id, EventStatus.PENDING, 2, Duration.ofMinutes(1))), LOCK_TOKEN
        );

        assertThat(distance(id, "updated_at")).isBetween(SLACK.negated(), SLACK);
        assertThat(repository.findAndLockBatchByStatus(EventStatus.PENDING, 100, EventStatus.PENDING))
                .filteredOn(event -> event.getId().equals(id))
                .singleElement()
                .extracting(OutboxEvent::getRetryCount)
                .isEqualTo(2);
    }

    public void partiallyUpdateBatch_whenDelayIsGiven_shouldCountNextRetryFromDatabaseTime() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        Duration delay = Duration.ofMinutes(10);

        repository.partiallyUpdateBatchByLockToken(
                List.of(buildEventWithRetry(id, EventStatus.PENDING, 1, delay)), LOCK_TOKEN
        );

        assertThat(distance(id, "next_retry_at")).isBetween(delay.minus(SLACK), delay.plus(SLACK));
    }

    public void partiallyUpdateBatch_whenDelayIsNull_shouldKeepNextRetryAt() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        clock.shift(TABLE, "next_retry_at", id, LONG_AGO);

        repository.partiallyUpdateBatchByLockToken(
                List.of(buildEventWithRetry(id, EventStatus.FAILED, 3, null)), LOCK_TOKEN
        );

        assertThat(distance(id, "next_retry_at")).isBetween(LONG_AGO.minus(SLACK), LONG_AGO.plus(SLACK));
    }

    public void partiallyUpdateBatch_emptyList_returnsZero() {
        assertThat(repository.partiallyUpdateBatchByLockToken(List.of(), LOCK_TOKEN)).isEqualTo(0);
    }

    public void partiallyUpdateBatch_nullList_returnsZero() {
        assertThat(repository.partiallyUpdateBatchByLockToken(null, LOCK_TOKEN)).isEqualTo(0);
    }

    public void partiallyUpdateBatch_multipleEvents_allUpdated() {
        List<UUID> ids = List.copyOf(capture(LOCK_TOKEN, 2));

        int count = repository.partiallyUpdateBatchByLockToken(List.of(
                buildEventWithRetry(ids.get(0), EventStatus.FAILED, 1, Duration.ofMinutes(1)),
                buildEventWithRetry(ids.get(1), EventStatus.FAILED, 2, Duration.ofMinutes(1))
        ), LOCK_TOKEN);

        assertThat(count).isEqualTo(2);
    }

    public void partiallyUpdateBatch_whenHeldByAnotherLockToken_updatesNothing() {
        UUID id = capture(LOCK_TOKEN, 1).iterator().next();
        List<OutboxEvent> outcome = List.of(buildEventWithRetry(id, EventStatus.FAILED, 1, Duration.ofMinutes(1)));

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

    public void updateBatchStatusByStatusOlderThan_whenSomeEventsAreOld_shouldUpdateOnlyThem() {
        OutboxEvent old = buildEvent(EventStatus.IN_PROCESS);
        OutboxEvent fresh = buildEvent(EventStatus.IN_PROCESS);
        repository.saveBatch(List.of(old, fresh));
        clock.shift(TABLE, "updated_at", old.getId(), LONG_AGO);

        int updated = repository.updateBatchStatusByStatusOlderThan(EventStatus.IN_PROCESS, AGE, 10, EventStatus.PENDING);

        assertThat(updated).isEqualTo(1);
        assertThat(repository.findAndLockBatchByStatus(EventStatus.PENDING, 10, EventStatus.IN_PROCESS))
                .extracting(OutboxEvent::getId)
                .containsExactly(old.getId());
    }

    public void updateBatchStatusByStatusOlderThan_whenNothingIsOldEnough_shouldReturnZero() {
        repository.saveBatch(List.of(buildEvent(EventStatus.IN_PROCESS)));

        int updated = repository.updateBatchStatusByStatusOlderThan(EventStatus.IN_PROCESS, AGE, 10, EventStatus.PENDING);

        assertThat(updated).isZero();
    }

    public void updateBatchStatusByStatusOlderThan_whenMoreEventsAreOld_shouldUpdateAtMostBatchSize() {
        List<OutboxEvent> events = IntStream.range(0, 5).mapToObj(i -> buildEvent(EventStatus.IN_PROCESS)).toList();
        repository.saveBatch(events);
        events.forEach(event -> clock.shift(TABLE, "updated_at", event.getId(), LONG_AGO));

        int updated = repository.updateBatchStatusByStatusOlderThan(EventStatus.IN_PROCESS, AGE, 3, EventStatus.PENDING);

        assertThat(updated).isEqualTo(3);
    }

    public void updateBatchStatusByStatusOlderThan_whenOldEventHasAnotherStatus_shouldLeaveIt() {
        OutboxEvent oldPending = buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(oldPending));
        clock.shift(TABLE, "updated_at", oldPending.getId(), LONG_AGO);

        int updated = repository.updateBatchStatusByStatusOlderThan(EventStatus.IN_PROCESS, AGE, 10, EventStatus.FAILED);

        assertThat(updated).isZero();
        assertThat(idsWithStatus(EventStatus.PENDING)).contains(oldPending.getId());
    }

    public void deleteBatchByStatusOlderThan_whenSomeEventsAreOld_shouldDeleteOnlyOldOfThatStatus() {
        OutboxEvent oldProcessed = buildEvent(EventStatus.PROCESSED);
        OutboxEvent freshProcessed = buildEvent(EventStatus.PROCESSED);
        OutboxEvent oldPending = buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(oldProcessed, freshProcessed, oldPending));
        clock.shift(TABLE, "updated_at", oldProcessed.getId(), LONG_AGO);
        clock.shift(TABLE, "updated_at", oldPending.getId(), LONG_AGO);

        int deleted = repository.deleteBatchByStatusOlderThan(EventStatus.PROCESSED, AGE, 10);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.deleteBatch(Set.of(oldProcessed.getId(), freshProcessed.getId(), oldPending.getId())))
                .isEqualTo(2);
    }

    public void deleteBatchByStatusOlderThan_whenMoreEventsAreOld_shouldDeleteAtMostBatchSize() {
        List<OutboxEvent> events = IntStream.range(0, 5).mapToObj(i -> buildEvent(EventStatus.PROCESSED)).toList();
        repository.saveBatch(events);
        events.forEach(event -> clock.shift(TABLE, "updated_at", event.getId(), LONG_AGO));

        int deleted = repository.deleteBatchByStatusOlderThan(EventStatus.PROCESSED, AGE, 3);

        assertThat(deleted).isEqualTo(3);
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
     * Saves {@code count} events of an event type of their own, due at once since the database stamps their next
     * retry time on insert, and captures them with the given token, the way a poller does - the finalization
     * methods only touch events captured with the token they are given.
     */
    private Set<UUID> capture(UUID lockToken, int count) {
        String eventType = "capture-" + UUID.randomUUID();
        List<OutboxEvent> events = IntStream.range(0, count)
                .mapToObj(i -> buildEventWithType(EventStatus.PENDING, eventType))
                .toList();
        repository.saveBatch(events);
        List<OutboxEvent> captured = repository.findAndLockBatchByEventTypeAndStatus(
                eventType, EventStatus.PENDING, count, lockToken, EventStatus.IN_PROCESS
        );
        assertThat(captured).hasSize(count);
        return captured.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
    }

    /**
     * The ids of the events in {@code status}, read by a capture that leaves them in it.
     */
    private List<UUID> idsWithStatus(EventStatus status) {
        return repository.findAndLockBatchByStatus(status, 100, status).stream()
                .map(OutboxEvent::getId)
                .toList();
    }

    private Duration distance(UUID id, String column) {
        return clock.distanceFromNow(TABLE, column, id);
    }

    public OutboxEvent buildEvent(EventStatus status) {
        return buildEventWithType(status, "ORDER_CREATED");
    }

    public OutboxEvent buildEventWithType(EventStatus status, String eventType) {
        return new OutboxEvent(
                UUID.randomUUID(), status, eventType,
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                -1, null, null, null
        );
    }

    private OutboxEvent buildEventWithCreatedAt(String eventType, Instant createdAt) {
        return new OutboxEvent(
                UUID.randomUUID(), EventStatus.PENDING, eventType,
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                -1, null, createdAt, null
        );
    }

    private OutboxEvent buildEventWithRetry(UUID id, EventStatus status, int retryCount, Duration retryDelay) {
        OutboxEvent event = new OutboxEvent(
                id, status, "ORDER_CREATED",
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                retryCount, null, null, null
        );
        event.setRetryDelay(retryDelay);
        return event;
    }
}
