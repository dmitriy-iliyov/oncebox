package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepositoryVerifier;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryVerifier;
import io.github.dmitriyiliyov.oncebox.core.outbox.AbstractOutboxRepositoryIntegrationTests;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepositoryVerifier;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxFeature;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

// The testkit repository verifiers of the dialect modules, run on the tables a schema migrator creates instead of the
// oncebox-test-fixtures scripts they run on there. The dialect ITs are @Transactional Spring tests; here every
// scenario runs in a TransactionTemplate rolled back at its end, which gives the verifiers the same single
// connection and transaction without a Spring context.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRepositoriesOnMigratedSchemaIntegrationTests {

    private TestkitDatabase database;
    private TransactionTemplate transactionTemplate;
    private AbstractOutboxRepositoryIntegrationTests outbox;
    private OutboxDlqRepositoryVerifier dlq;
    private InboxRepositoryVerifier inbox;
    private DistributedLockRepositoryVerifier locks;

    abstract TestkitDialect dialect();

    abstract TestkitMigrator migrator();

    @BeforeAll
    void migrateSchema() {
        TestkitDialect dialect = dialect();
        DataSource dataSource = dialect.dataSource();
        database = new TestkitDatabase(dialect, dataSource);
        database.dropOnceboxTables();
        OnceboxSchemaMigrator migrator = migrator().create(dataSource, dialect.vendor());
        for (OnceboxFeature feature : OnceboxFeature.values()) {
            migrator.migrate(feature.getValue());
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        OnceboxRepositoryFactory factory = dialect.repositoryFactory(jdbcTemplate);
        TestDatabaseClock clock = dialect.clock(jdbcTemplate);
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        outbox = new AbstractOutboxRepositoryIntegrationTests(factory.createOutboxRepository(), clock);
        dlq = new OutboxDlqRepositoryVerifier(
                factory.createOutboxDlqRepository(),
                jdbcTemplate,
                dialect.uuidHelper(),
                dialect.resultSetMapper(),
                clock
        );
        inbox = new InboxRepositoryVerifier(factory.createInboxRepository(), clock);
        locks = new DistributedLockRepositoryVerifier(
                jdbcTemplate,
                factory.createDistributedLockRepository(),
                dialect.idExtractor(),
                dialect.idPreparer(),
                dialect.setupJobScript()
        );
    }

    @BeforeEach
    void emptyTables() {
        database.emptyOnceboxTables();
    }

    @AfterAll
    void dropSchema() {
        if (database != null) {
            database.dropOnceboxTables();
        }
    }

    // OutboxRepository

    @Test
    @DisplayName("IT OutboxRepository.save() should persist event correctly")
    void outboxSave_singleEvent_persistedCorrectly() {
        inRolledBackTransaction(outbox::save_singleEvent_persistedCorrectly);
    }

    @Test
    @DisplayName("IT OutboxRepository.save() when the event is new should leave every time to the database")
    void outboxSave_whenEventIsNew_shouldLeaveEveryTimeToDatabase() {
        inRolledBackTransaction(outbox::save_whenEventIsNew_shouldLeaveEveryTimeToDatabase);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatch() should persist all events")
    void outboxSaveBatch_multipleEvents_allPersisted() {
        inRolledBackTransaction(outbox::saveBatch_multipleEvents_allPersisted);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatch() with empty list should not throw")
    void outboxSaveBatch_emptyList_doesNotThrow() {
        inRolledBackTransaction(outbox::saveBatch_emptyList_doesNotThrow);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatchWithCreatedAt() should keep the creation time and make the event due at"
            + " once")
    void outboxSaveBatchWithCreatedAt_whenEventHasCreatedAt_shouldKeepItAndMakeEventDueAtOnce() {
        inRolledBackTransaction(outbox::saveBatchWithCreatedAt_whenEventHasCreatedAt_shouldKeepItAndMakeEventDueAtOnce);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatchWithCreatedAt() when read back in another JVM time zone should give"
            + " back the same moment")
    void outboxSaveBatchWithCreatedAt_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoment() {
        inRolledBackTransaction(outbox::saveBatchWithCreatedAt_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoment);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatchWithCreatedAt() when an event has no creation time should throw and"
            + " save nothing")
    void outboxSaveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowAndSaveNothing() {
        inRolledBackTransaction(outbox::saveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowAndSaveNothing);
    }

    @Test
    @DisplayName("IT OutboxRepository.saveBatchWithCreatedAt() when the batch is empty should not throw")
    void outboxSaveBatchWithCreatedAt_whenBatchIsEmpty_shouldNotThrow() {
        inRolledBackTransaction(outbox::saveBatchWithCreatedAt_whenBatchIsEmpty_shouldNotThrow);
    }

    @Test
    @DisplayName("IT OutboxRepository.findAndLockBatchByEventTypeAndStatus() when the next retry is ahead should skip"
            + " the event")
    void outboxFindAndLockBatchByEventTypeAndStatus_whenNextRetryIsAhead_shouldSkipEvent() {
        inRolledBackTransaction(outbox::findAndLockBatchByEventTypeAndStatus_whenNextRetryIsAhead_shouldSkipEvent);
    }

    @Test
    @DisplayName("IT OutboxRepository.findAndLockBatchByEventTypeAndStatus() should stamp updated_at by the database"
            + " clock")
    void outboxFindAndLockBatchByEventTypeAndStatus_whenCaptured_shouldStampUpdatedAtByDatabaseClock() {
        inRolledBackTransaction(
                outbox::findAndLockBatchByEventTypeAndStatus_whenCaptured_shouldStampUpdatedAtByDatabaseClock);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() to PENDING should update all")
    void outboxUpdateBatchStatus_toPending_updatesAll() {
        inRolledBackTransaction(outbox::updateBatchStatus_toPending_updatesAll);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() to PROCESSED should update all")
    void outboxUpdateBatchStatus_toProcessed_updatesAll() {
        inRolledBackTransaction(outbox::updateBatchStatus_toProcessed_updatesAll);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() to FAILED should throw exception")
    void outboxUpdateBatchStatus_toFailed_throwsException() {
        inRolledBackTransaction(outbox::updateBatchStatus_toFailed_throwsException);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() with empty ids should return zero")
    void outboxUpdateBatchStatus_emptyIds_returnsZero() {
        inRolledBackTransaction(outbox::updateBatchStatus_emptyIds_returnsZero);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() should not affect other events")
    void outboxUpdateBatchStatus_doesNotAffectOtherEvents() {
        inRolledBackTransaction(outbox::updateBatchStatus_doesNotAffectOtherEvents);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() when held by another lock token should update"
            + " nothing")
    void outboxUpdateBatchStatus_whenHeldByAnotherLockToken_updatesNothing() {
        inRolledBackTransaction(outbox::updateBatchStatus_whenHeldByAnotherLockToken_updatesNothing);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() when held by the token should write the status"
            + " and stamp updated_at by the database clock")
    void outboxUpdateBatchStatus_whenHeldByLockToken_shouldWriteStatusAndStampUpdatedAtByDatabaseClock() {
        inRolledBackTransaction(
                outbox::updateBatchStatus_whenHeldByLockToken_shouldWriteStatusAndStampUpdatedAtByDatabaseClock);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByLockToken() when the event was never captured should update"
            + " nothing")
    void outboxUpdateBatchStatus_whenNeverCaptured_updatesNothing() {
        inRolledBackTransaction(outbox::updateBatchStatus_whenNeverCaptured_updatesNothing);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() should increment retry count")
    void outboxPartiallyUpdateBatch_incrementsRetryCount() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_incrementsRetryCount);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() when held by the token should write the retry"
            + " state and stamp updated_at by the database clock")
    void outboxPartiallyUpdateBatch_whenHeldByLockToken_shouldWriteRetryStateAndStampUpdatedAtByDatabaseClock() {
        inRolledBackTransaction(
                outbox::partiallyUpdateBatch_whenHeldByLockToken_shouldWriteRetryStateAndStampUpdatedAtByDatabaseClock);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() when a delay is given should count the next"
            + " retry from the database time")
    void outboxPartiallyUpdateBatch_whenDelayIsGiven_shouldCountNextRetryFromDatabaseTime() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_whenDelayIsGiven_shouldCountNextRetryFromDatabaseTime);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() when the delay is null should keep"
            + " next_retry_at")
    void outboxPartiallyUpdateBatch_whenDelayIsNull_shouldKeepNextRetryAt() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_whenDelayIsNull_shouldKeepNextRetryAt);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() with empty list should return zero")
    void outboxPartiallyUpdateBatch_emptyList_returnsZero() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_emptyList_returnsZero);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() with null should return zero")
    void outboxPartiallyUpdateBatch_nullList_returnsZero() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_nullList_returnsZero);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() should update all events")
    void outboxPartiallyUpdateBatch_multipleEvents_allUpdated() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_multipleEvents_allUpdated);
    }

    @Test
    @DisplayName("IT OutboxRepository.partiallyUpdateBatchByLockToken() when held by another lock token should update"
            + " nothing")
    void outboxPartiallyUpdateBatch_whenHeldByAnotherLockToken_updatesNothing() {
        inRolledBackTransaction(outbox::partiallyUpdateBatch_whenHeldByAnotherLockToken_updatesNothing);
    }

    @Test
    @DisplayName("IT OutboxRepository.findAndLockBatchByStatus() should keep the lock token of the last capture")
    void outboxFindAndLockBatchByStatus_shouldKeepLockTokenOfLastCapture() {
        inRolledBackTransaction(outbox::findAndLockBatchByStatus_shouldKeepLockTokenOfLastCapture);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByStatusOlderThan() when some events are old should update only"
            + " them")
    void outboxUpdateBatchStatusByStatusOlderThan_whenSomeEventsAreOld_shouldUpdateOnlyThem() {
        inRolledBackTransaction(outbox::updateBatchStatusByStatusOlderThan_whenSomeEventsAreOld_shouldUpdateOnlyThem);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByStatusOlderThan() when nothing is old enough should return"
            + " zero")
    void outboxUpdateBatchStatusByStatusOlderThan_whenNothingIsOldEnough_shouldReturnZero() {
        inRolledBackTransaction(outbox::updateBatchStatusByStatusOlderThan_whenNothingIsOldEnough_shouldReturnZero);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByStatusOlderThan() should update at most batchSize events")
    void outboxUpdateBatchStatusByStatusOlderThan_whenMoreEventsAreOld_shouldUpdateAtMostBatchSize() {
        inRolledBackTransaction(
                outbox::updateBatchStatusByStatusOlderThan_whenMoreEventsAreOld_shouldUpdateAtMostBatchSize);
    }

    @Test
    @DisplayName("IT OutboxRepository.updateBatchStatusByStatusOlderThan() when an old event has another status should"
            + " leave it")
    void outboxUpdateBatchStatusByStatusOlderThan_whenOldEventHasAnotherStatus_shouldLeaveIt() {
        inRolledBackTransaction(outbox::updateBatchStatusByStatusOlderThan_whenOldEventHasAnotherStatus_shouldLeaveIt);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatchByStatusOlderThan() should delete only the old events of that status")
    void outboxDeleteBatchByStatusOlderThan_whenSomeEventsAreOld_shouldDeleteOnlyOldOfThatStatus() {
        inRolledBackTransaction(
                outbox::deleteBatchByStatusOlderThan_whenSomeEventsAreOld_shouldDeleteOnlyOldOfThatStatus);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatchByStatusOlderThan() should delete at most batchSize events")
    void outboxDeleteBatchByStatusOlderThan_whenMoreEventsAreOld_shouldDeleteAtMostBatchSize() {
        inRolledBackTransaction(outbox::deleteBatchByStatusOlderThan_whenMoreEventsAreOld_shouldDeleteAtMostBatchSize);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatch() should delete and return count")
    void outboxDeleteBatch_existingIds_deletedAndReturnsCount() {
        inRolledBackTransaction(outbox::deleteBatch_existingIds_deletedAndReturnsCount);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatch() with empty ids should return zero")
    void outboxDeleteBatch_emptyIds_returnsZero() {
        inRolledBackTransaction(outbox::deleteBatch_emptyIds_returnsZero);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatch() should not affect other events")
    void outboxDeleteBatch_doesNotAffectOtherEvents() {
        inRolledBackTransaction(outbox::deleteBatch_doesNotAffectOtherEvents);
    }

    @Test
    @DisplayName("IT OutboxRepository.deleteBatch() with not existing ids should return zero")
    void outboxDeleteBatch_notExistingIds_returnsZero() {
        inRolledBackTransaction(outbox::deleteBatch_notExistingIds_returnsZero);
    }

    // OutboxDlqRepository

    @Test
    @DisplayName("IT OutboxDlqRepository.saveBatch() when an event is moved should keep its times and stamp moved_at"
            + " by the database clock")
    void dlqSaveBatch_whenEventIsMoved_shouldKeepItsTimesAndStampMovedAtByDatabaseClock() {
        inRolledBackTransaction(dlq::saveBatch_whenEventIsMoved_shouldKeepItsTimesAndStampMovedAtByDatabaseClock);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.saveBatch() when read back in another JVM time zone should give back the same"
            + " moments")
    void dlqSaveBatch_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoments() {
        inRolledBackTransaction(dlq::saveBatch_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoments);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.saveBatch() with single event should persist correctly")
    void dlqSaveBatch_singleEvent_persistedCorrectly() {
        inRolledBackTransaction(dlq::saveBatch_singleEvent_persistedCorrectly);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.saveBatch() with multiple events should persist all")
    void dlqSaveBatch_multipleEvents_allPersisted() {
        inRolledBackTransaction(dlq::saveBatch_multipleEvents_allPersisted);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.findAndLockBatchByStatus() when some events match should lock only them in"
            + " the lock status")
    void dlqFindAndLockBatchByStatus_whenSomeEventsMatch_shouldLockOnlyThemInLockStatus() {
        inRolledBackTransaction(dlq::findAndLockBatchByStatus_whenSomeEventsMatch_shouldLockOnlyThemInLockStatus);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.findAndLockBatchByStatus() when more events match should lock at most"
            + " batchSize")
    void dlqFindAndLockBatchByStatus_whenMoreEventsMatch_shouldLockAtMostBatchSize() {
        inRolledBackTransaction(dlq::findAndLockBatchByStatus_whenMoreEventsMatch_shouldLockAtMostBatchSize);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatch() for existing ids should delete and return count")
    void dlqDeleteBatch_existingIds_deletedAndReturnsCount() {
        inRolledBackTransaction(dlq::deleteBatch_existingIds_deletedAndReturnsCount);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatch() with empty ids should return zero")
    void dlqDeleteBatch_emptyIds_returnsZero() {
        inRolledBackTransaction(dlq::deleteBatch_emptyIds_returnsZero);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatch() should not affect other events")
    void dlqDeleteBatch_doesNotAffectOtherEvents() {
        inRolledBackTransaction(dlq::deleteBatch_doesNotAffectOtherEvents);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatchByStatusOlderThan() should delete matching events and return count")
    void dlqDeleteBatchByStatusOlderThan_matches_deleted() {
        inRolledBackTransaction(dlq::deleteBatchByStatusOlderThan_matches_deleted);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatchByStatusOlderThan() should not delete events newer than threshold")
    void dlqDeleteBatchByStatusOlderThan_newerThanAge_notDeleted() {
        inRolledBackTransaction(dlq::deleteBatchByStatusOlderThan_newerThanAge_notDeleted);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatchByStatusOlderThan() should not delete events with different status")
    void dlqDeleteBatchByStatusOlderThan_wrongStatus_notDeleted() {
        inRolledBackTransaction(dlq::deleteBatchByStatusOlderThan_wrongStatus_notDeleted);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatchByStatusOlderThan() should respect batch size limit")
    void dlqDeleteBatchByStatusOlderThan_respectsBatchSize() {
        inRolledBackTransaction(dlq::deleteBatchByStatusOlderThan_respectsBatchSize);
    }

    @Test
    @DisplayName("IT OutboxDlqRepository.deleteBatchByStatusOlderThan() when no matches should return zero")
    void dlqDeleteBatchByStatusOlderThan_noMatches_returnsZero() {
        inRolledBackTransaction(dlq::deleteBatchByStatusOlderThan_noMatches_returnsZero);
    }

    // InboxRepository

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(UUID) when the id is new should return 1 and stamp consumed_at by"
            + " the database clock")
    void inboxSaveIfAbsent_whenIdIsNew_shouldReturnOneAndStampConsumedAtByDatabaseClock() {
        inRolledBackTransaction(inbox::saveIfAbsent_whenIdIsNew_shouldReturnOneAndStampConsumedAtByDatabaseClock);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(UUID) when the id is already recorded should return 0")
    void inboxSaveIfAbsent_whenIdIsAlreadyRecorded_shouldReturnZero() {
        inRolledBackTransaction(inbox::saveIfAbsent_whenIdIsAlreadyRecorded_shouldReturnZero);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when every id is new should return all of them and stamp"
            + " consumed_at")
    void inboxSaveIfAbsentBatch_whenEveryIdIsNew_shouldReturnAllAndStampConsumedAt() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenEveryIdIsNew_shouldReturnAllAndStampConsumedAt);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when some ids are recorded should return only the new ones")
    void inboxSaveIfAbsentBatch_whenSomeIdsAreRecorded_shouldReturnOnlyNewOnes() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenSomeIdsAreRecorded_shouldReturnOnlyNewOnes);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when every id is recorded should return empty set")
    void inboxSaveIfAbsentBatch_whenEveryIdIsRecorded_shouldReturnEmptySet() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenEveryIdIsRecorded_shouldReturnEmptySet);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when ids are empty should return empty set")
    void inboxSaveIfAbsentBatch_whenIdsAreEmpty_shouldReturnEmptySet() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenIdsAreEmpty_shouldReturnEmptySet);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when ids are null should throw NullPointerException")
    void inboxSaveIfAbsentBatch_whenIdsAreNull_shouldThrowNullPointerException() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenIdsAreNull_shouldThrowNullPointerException);
    }

    @Test
    @DisplayName("IT InboxRepository.saveIfAbsent(Set) when the batch is large should record every id")
    void inboxSaveIfAbsentBatch_whenBatchIsLarge_shouldRecordEveryId() {
        inRolledBackTransaction(inbox::saveIfAbsentBatch_whenBatchIsLarge_shouldRecordEveryId);
    }

    @Test
    @DisplayName("IT InboxRepository.deleteBatchOlderThan() should delete the records consumed longer than age ago and"
            + " keep the fresh")
    void inboxDeleteBatchOlderThan_whenSomeRecordsAreOld_shouldDeleteOnlyThem() {
        inRolledBackTransaction(inbox::deleteBatchOlderThan_whenSomeRecordsAreOld_shouldDeleteOnlyThem);
    }

    @Test
    @DisplayName("IT InboxRepository.deleteBatchOlderThan() when every record is fresh should delete nothing")
    void inboxDeleteBatchOlderThan_whenEveryRecordIsFresh_shouldDeleteNothing() {
        inRolledBackTransaction(inbox::deleteBatchOlderThan_whenEveryRecordIsFresh_shouldDeleteNothing);
    }

    @Test
    @DisplayName("IT InboxRepository.deleteBatchOlderThan() should delete at most batchSize records")
    void inboxDeleteBatchOlderThan_whenMoreRecordsAreOld_shouldDeleteAtMostBatchSize() {
        inRolledBackTransaction(inbox::deleteBatchOlderThan_whenMoreRecordsAreOld_shouldDeleteAtMostBatchSize);
    }

    @Test
    @DisplayName("IT InboxRepository.deleteBatchOlderThan() when there are no records should return zero")
    void inboxDeleteBatchOlderThan_whenThereAreNoRecords_shouldReturnZero() {
        inRolledBackTransaction(inbox::deleteBatchOlderThan_whenThereAreNoRecords_shouldReturnZero);
    }

    @Test
    @DisplayName("IT InboxRepository.deleteBatchOlderThan() when the JVM runs in another time zone should still keep a"
            + " fresh record")
    void inboxDeleteBatchOlderThan_whenJvmRunsInAnotherZone_shouldKeepFreshRecord() {
        inRolledBackTransaction(inbox::deleteBatchOlderThan_whenJvmRunsInAnotherZone_shouldKeepFreshRecord);
    }

    // DistributedLockRepository

    @Test
    @DisplayName("IT DistributedLockRepository.tryLock() should lock job when available")
    void lockTryLock_jobAvailable_locksSuccessfully() {
        inRolledBackTransaction(locks::tryLock_jobAvailable_locksSuccessfully);
    }

    @Test
    @DisplayName("IT DistributedLockRepository.tryLock() should return false when job already locked")
    void lockTryLock_jobAlreadyLocked_returnsFalse() {
        inRolledBackTransaction(locks::tryLock_jobAlreadyLocked_returnsFalse);
    }

    @Test
    @DisplayName("IT DistributedLockRepository.tryLock() should lock job when lock expired")
    void lockTryLock_lockExpired_locksSuccessfully() {
        inRolledBackTransaction(locks::tryLock_lockExpired_locksSuccessfully);
    }

    @Test
    @DisplayName("IT DistributedLockRepository.unlock() should unlock job for valid worker")
    void lockUnlock_validWorker_unlocksJob() {
        inRolledBackTransaction(locks::unlock_validWorker_unlocksJob);
    }

    @Test
    @DisplayName("IT DistributedLockRepository.unlock() should not unlock for different worker")
    void lockUnlock_differentWorker_doesNotUnlock() {
        inRolledBackTransaction(locks::unlock_differentWorker_doesNotUnlock);
    }

    @Test
    @DisplayName("IT DistributedLockRepository.tryLock() should return false for non-existent job")
    void lockTryLock_nonExistentJob_returnsFalse() {
        inRolledBackTransaction(locks::tryLock_nonExistentJob_returnsFalse);
    }

    private void inRolledBackTransaction(Runnable scenario) {
        transactionTemplate.executeWithoutResult(status -> {
            status.setRollbackOnly();
            scenario.run();
        });
    }
}
