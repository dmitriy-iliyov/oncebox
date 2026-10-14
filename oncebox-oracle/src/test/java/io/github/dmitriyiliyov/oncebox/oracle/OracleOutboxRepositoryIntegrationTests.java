package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.outbox.AbstractOutboxRepositoryIntegrationTests;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OracleOutboxRepositoryIntegrationTests extends BaseOracleIntegrationTests {

    private final OracleOutboxRepository repository;
    private final AbstractOutboxRepositoryIntegrationTests delegate;

    private final JdbcTemplate jdbcTemplate;
    private final OracleSqlUuidHelper oracleSqlUuidHelper = new OracleSqlUuidHelper();
    private final DefaultBytesResultSetMapper mapper = new DefaultBytesResultSetMapper();

    OracleOutboxRepositoryIntegrationTests(
            @Qualifier("oracleOutboxRepository") OracleOutboxRepository repository,
            @Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.delegate = new AbstractOutboxRepositoryIntegrationTests(repository, new OracleDatabaseClock(jdbcTemplate));
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OracleOutboxRepository(jdbcTemplate, oracleSqlUuidHelper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mapper cannot be null");
    }

    @Test @DisplayName("IT save() when the event is new should leave every time to the database")
    void save_whenEventIsNew_shouldLeaveEveryTimeToDatabase() { delegate.save_whenEventIsNew_shouldLeaveEveryTimeToDatabase(); }

    @Test @DisplayName("IT saveBatchWithCreatedAt() should keep the creation time and make the event due at once")
    void saveBatchWithCreatedAt_whenEventHasCreatedAt_shouldKeepItAndMakeEventDueAtOnce() { delegate.saveBatchWithCreatedAt_whenEventHasCreatedAt_shouldKeepItAndMakeEventDueAtOnce(); }

    @Test @DisplayName("IT saveBatchWithCreatedAt() when read back in another JVM time zone should give back the same moment")
    void saveBatchWithCreatedAt_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoment() { delegate.saveBatchWithCreatedAt_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoment(); }

    @Test @DisplayName("IT saveBatchWithCreatedAt() when an event has no creation time should throw and save nothing")
    void saveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowAndSaveNothing() { delegate.saveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowAndSaveNothing(); }

    @Test @DisplayName("IT findAndLockBatchByEventTypeAndStatus() when the next retry is ahead should skip the event")
    void findAndLockBatchByEventTypeAndStatus_whenNextRetryIsAhead_shouldSkipEvent() { delegate.findAndLockBatchByEventTypeAndStatus_whenNextRetryIsAhead_shouldSkipEvent(); }

    @Test @DisplayName("IT findAndLockBatchByEventTypeAndStatus() should stamp updated_at by the database clock")
    void findAndLockBatchByEventTypeAndStatus_whenCaptured_shouldStampUpdatedAtByDatabaseClock() { delegate.findAndLockBatchByEventTypeAndStatus_whenCaptured_shouldStampUpdatedAtByDatabaseClock(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() when a delay is given should count the next retry from the database time")
    void partiallyUpdateBatchByLockToken_whenDelayIsGiven_shouldCountNextRetryFromDatabaseTime() { delegate.partiallyUpdateBatch_whenDelayIsGiven_shouldCountNextRetryFromDatabaseTime(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() when the delay is null should keep next_retry_at")
    void partiallyUpdateBatchByLockToken_whenDelayIsNull_shouldKeepNextRetryAt() { delegate.partiallyUpdateBatch_whenDelayIsNull_shouldKeepNextRetryAt(); }

    @Test @DisplayName("IT updateBatchStatusByStatusOlderThan() when some events are old should update only them")
    void updateBatchStatusByStatusOlderThan_whenSomeEventsAreOld_shouldUpdateOnlyThem() { delegate.updateBatchStatusByStatusOlderThan_whenSomeEventsAreOld_shouldUpdateOnlyThem(); }

    @Test @DisplayName("IT updateBatchStatusByStatusOlderThan() when nothing is old enough should return zero")
    void updateBatchStatusByStatusOlderThan_whenNothingIsOldEnough_shouldReturnZero() { delegate.updateBatchStatusByStatusOlderThan_whenNothingIsOldEnough_shouldReturnZero(); }

    @Test @DisplayName("IT updateBatchStatusByStatusOlderThan() should update at most batchSize events")
    void updateBatchStatusByStatusOlderThan_whenMoreEventsAreOld_shouldUpdateAtMostBatchSize() { delegate.updateBatchStatusByStatusOlderThan_whenMoreEventsAreOld_shouldUpdateAtMostBatchSize(); }

    @Test @DisplayName("IT deleteBatchByStatusOlderThan() should delete only the old events of that status")
    void deleteBatchByStatusOlderThan_whenSomeEventsAreOld_shouldDeleteOnlyOldOfThatStatus() { delegate.deleteBatchByStatusOlderThan_whenSomeEventsAreOld_shouldDeleteOnlyOldOfThatStatus(); }

    @Test @DisplayName("IT deleteBatchByStatusOlderThan() should delete at most batchSize events")
    void deleteBatchByStatusOlderThan_whenMoreEventsAreOld_shouldDeleteAtMostBatchSize() { delegate.deleteBatchByStatusOlderThan_whenMoreEventsAreOld_shouldDeleteAtMostBatchSize(); }

    @Test @DisplayName("IT save() should persist event correctly")
    void save_singleEvent_persistedCorrectly() { delegate.save_singleEvent_persistedCorrectly(); }

    @Test @DisplayName("IT saveBatch() should persist all events")
    void saveBatch_multipleEvents_allPersisted() { delegate.saveBatch_multipleEvents_allPersisted(); }

    @Test @DisplayName("IT saveBatch() with empty list should not throw")
    void saveBatch_emptyList_doesNotThrow() { delegate.saveBatch_emptyList_doesNotThrow(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() to PENDING should update all")
    void updateBatchStatusByLockToken_toPending_updatesAll() { delegate.updateBatchStatus_toPending_updatesAll(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() to PROCESSED should update all")
    void updateBatchStatusByLockToken_toProcessed_updatesAll() { delegate.updateBatchStatus_toProcessed_updatesAll(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() to FAILED should throw exception")
    void updateBatchStatusByLockToken_toFailed_throwsException() { delegate.updateBatchStatus_toFailed_throwsException(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() with empty ids should return zero")
    void updateBatchStatusByLockToken_emptyIds_returnsZero() { delegate.updateBatchStatus_emptyIds_returnsZero(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() should not affect other events")
    void updateBatchStatusByLockToken_doesNotAffectOtherEvents() { delegate.updateBatchStatus_doesNotAffectOtherEvents(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() should increment retry count")
    void partiallyUpdateBatchByLockToken_incrementsRetryCount() { delegate.partiallyUpdateBatch_incrementsRetryCount(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() with empty list should return zero")
    void partiallyUpdateBatchByLockToken_emptyList_returnsZero() { delegate.partiallyUpdateBatch_emptyList_returnsZero(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() with null should return zero")
    void partiallyUpdateBatchByLockToken_nullList_returnsZero() { delegate.partiallyUpdateBatch_nullList_returnsZero(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() should update all events")
    void partiallyUpdateBatchByLockToken_multipleEvents_allUpdated() { delegate.partiallyUpdateBatch_multipleEvents_allUpdated(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() when held by another lock token should update nothing")
    void updateBatchStatusByLockToken_whenHeldByAnotherLockToken_updatesNothing() { delegate.updateBatchStatus_whenHeldByAnotherLockToken_updatesNothing(); }

    @Test @DisplayName("IT updateBatchStatusByLockToken() when the event was never captured should update nothing")
    void updateBatchStatusByLockToken_whenNeverCaptured_updatesNothing() { delegate.updateBatchStatus_whenNeverCaptured_updatesNothing(); }

    @Test @DisplayName("IT partiallyUpdateBatchByLockToken() when held by another lock token should update nothing")
    void partiallyUpdateBatchByLockToken_whenHeldByAnotherLockToken_updatesNothing() { delegate.partiallyUpdateBatch_whenHeldByAnotherLockToken_updatesNothing(); }

    @Test @DisplayName("IT findAndLockBatchByStatus() should keep the lock token of the last capture")
    void findAndLockBatchByStatus_shouldKeepLockTokenOfLastCapture() { delegate.findAndLockBatchByStatus_shouldKeepLockTokenOfLastCapture(); }

    @Test @DisplayName("IT deleteBatch() should delete and return count")
    void deleteBatch_existingIds_deletedAndReturnsCount() { delegate.deleteBatch_existingIds_deletedAndReturnsCount(); }

    @Test @DisplayName("IT deleteBatch() with empty ids should return zero")
    void deleteBatch_emptyIds_returnsZero() { delegate.deleteBatch_emptyIds_returnsZero(); }

    @Test @DisplayName("IT deleteBatch() should not affect other events")
    void deleteBatch_doesNotAffectOtherEvents() { delegate.deleteBatch_doesNotAffectOtherEvents(); }

    @Test @DisplayName("IT deleteBatch() with not existing ids should return zero")
    void deleteBatch_notExistingIds_returnsZero() { delegate.deleteBatch_notExistingIds_returnsZero(); }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should lock and set status via subquery + UPDATE")
    void findAndLockBatchByStatus_locksAndSetsStatus() {
        OutboxEvent e1 = delegate.buildEvent(EventStatus.PENDING);
        OutboxEvent e2 = delegate.buildEvent(EventStatus.PENDING);
        repository.saveBatch(List.of(e1, e2));

        List<OutboxEvent> locked = repository
                .findAndLockBatchByStatus(EventStatus.PENDING, 10, EventStatus.IN_PROCESS);

        assertThat(locked)
                .hasSize(2)
                .extracting(OutboxEvent::getStatus)
                .containsOnly(EventStatus.IN_PROCESS);
        assertThat(locked)
                .extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should respect batch size via FETCH FIRST")
    void findAndLockBatchByStatus_respectsBatchSize() {
        repository.saveBatch(
                IntStream.range(0, 5)
                        .mapToObj(i -> delegate.buildEvent(EventStatus.PENDING))
                        .toList()
        );

        List<OutboxEvent> locked = repository
                .findAndLockBatchByStatus(EventStatus.PENDING, 3, EventStatus.IN_PROCESS);

        assertThat(locked).hasSize(3);
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() when no matches should return empty")
    void findAndLockBatchByStatus_noMatches_returnsEmpty() {
        assertThat(repository.findAndLockBatchByStatus(
                EventStatus.PENDING, 10, EventStatus.IN_PROCESS)
        ).isEmpty();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should not lock events of other status")
    void findAndLockBatchByStatus_doesNotLockOtherStatuses() {
        OutboxEvent pending   = delegate.buildEvent(EventStatus.PENDING);
        OutboxEvent processed = delegate.buildEvent(EventStatus.PROCESSED);
        repository.saveBatch(List.of(pending, processed));

        repository.findAndLockBatchByStatus(EventStatus.PENDING, 10, EventStatus.IN_PROCESS);

        List<OutboxEvent> stillProcessed = repository
                .findAndLockBatchByStatus(EventStatus.PROCESSED, 10, EventStatus.IN_PROCESS);
        assertThat(stillProcessed)
                .extracting(OutboxEvent::getId)
                .contains(processed.getId());
    }

    @Test
    @DisplayName("IT findAndLockBatchByEventTypeAndStatus() should lock only matching event type")
    void findAndLockBatchByEventTypeAndStatus_locksOnlyMatchingType() {
        OutboxEvent orderEvent   = delegate.buildEvent(EventStatus.PENDING);
        OutboxEvent paymentEvent = delegate.buildEventWithType(EventStatus.PENDING, "PAYMENT_CREATED");
        repository.saveBatch(List.of(orderEvent, paymentEvent));

        List<OutboxEvent> locked = repository.findAndLockBatchByEventTypeAndStatus(
                "ORDER_CREATED", EventStatus.PENDING, 10, UUID.randomUUID(), EventStatus.IN_PROCESS
        );

        assertThat(locked)
                .extracting(OutboxEvent::getId)
                .containsOnly(orderEvent.getId())
                .doesNotContain(paymentEvent.getId());
    }

    @Test
    @DisplayName("IT saveBatchWithCreatedAt() when the batch is empty should not throw")
    void saveBatchWithCreatedAt_whenBatchIsEmpty_shouldNotThrow() {
        delegate.saveBatchWithCreatedAt_whenBatchIsEmpty_shouldNotThrow();
    }

    @Test
    @DisplayName("IT updateBatchStatusByLockToken() when held by the token should write the status and stamp updated_at by the database clock")
    void updateBatchStatusByLockToken_whenHeldByLockToken_shouldWriteStatusAndStampUpdatedAtByDatabaseClock() {
        delegate.updateBatchStatus_whenHeldByLockToken_shouldWriteStatusAndStampUpdatedAtByDatabaseClock();
    }

    @Test
    @DisplayName("IT partiallyUpdateBatchByLockToken() when held by the token should write the retry state and stamp updated_at by the database clock")
    void partiallyUpdateBatchByLockToken_whenHeldByLockToken_shouldWriteRetryStateAndStampUpdatedAtByDatabaseClock() {
        delegate.partiallyUpdateBatch_whenHeldByLockToken_shouldWriteRetryStateAndStampUpdatedAtByDatabaseClock();
    }

    @Test
    @DisplayName("IT updateBatchStatusByStatusOlderThan() when an old event has another status should leave it")
    void updateBatchStatusByStatusOlderThan_whenOldEventHasAnotherStatus_shouldLeaveIt() {
        delegate.updateBatchStatusByStatusOlderThan_whenOldEventHasAnotherStatus_shouldLeaveIt();
    }
}
