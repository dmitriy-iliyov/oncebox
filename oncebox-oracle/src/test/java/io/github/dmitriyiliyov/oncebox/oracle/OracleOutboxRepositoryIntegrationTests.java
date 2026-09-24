package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.publisher.AbstractOutboxRepositoryIntegrationTests;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final Clock clock = Clock.systemUTC();
    private final OracleSqlUuidHelper oracleSqlUuidHelper = new OracleSqlUuidHelper();
    private final DefaultBytesResultSetMapper mapper = new DefaultBytesResultSetMapper();

    OracleOutboxRepositoryIntegrationTests(
            @Qualifier("oracleOutboxRepository") OracleOutboxRepository repository,
            @Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.delegate = new AbstractOutboxRepositoryIntegrationTests(repository);
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OracleOutboxRepository(jdbcTemplate, clock, oracleSqlUuidHelper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mapper cannot be null");
    }

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
        Instant pastRetryAt = Instant.now().minusSeconds(10).truncatedTo(ChronoUnit.MILLIS);

        OutboxEvent orderEvent   = delegate.buildEventWithNextRetryAt(EventStatus.PENDING, pastRetryAt);
        OutboxEvent paymentEvent = delegate.buildEventWithTypeAndNextRetryAt(
                EventStatus.PENDING, "PAYMENT_CREATED", pastRetryAt
        );
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
    @DisplayName("IT findAndLockBatchByEventTypeAndStatus() should respect next_retry_at threshold")
    void findAndLockBatchByEventTypeAndStatus_respectsNextRetryAt() {
        OutboxEvent ready = delegate.buildEventWithNextRetryAt(
                EventStatus.PENDING, Instant.now().minusSeconds(10).truncatedTo(ChronoUnit.MILLIS)
        );
        OutboxEvent notReady = delegate.buildEventWithNextRetryAt(
                EventStatus.PENDING, Instant.now().plusSeconds(300).truncatedTo(ChronoUnit.MILLIS)
        );
        repository.saveBatch(List.of(ready, notReady));

        List<OutboxEvent> locked = repository.findAndLockBatchByEventTypeAndStatus(
                "ORDER_CREATED", EventStatus.PENDING, 10, UUID.randomUUID(), EventStatus.IN_PROCESS
        );

        assertThat(locked)
                .extracting(OutboxEvent::getId)
                .contains(ready.getId())
                .doesNotContain(notReady.getId());
    }

    @Test
    @DisplayName("IT updateBatchStatusByStatusAndThreshold() should update only old events via SELECT + UPDATE")
    void updateBatchStatusByStatusAndThreshold_updatesOnlyOldEvents() {
        repository.saveBatch(List.of(delegate.buildEvent(EventStatus.IN_PROCESS)));

        Instant threshold = Instant.now().plusSeconds(1);
        int updated = repository.updateBatchStatusByStatusAndThreshold(
                EventStatus.IN_PROCESS, threshold, 10, EventStatus.PENDING
        );

        assertThat(updated).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("IT updateBatchStatusByStatusAndThreshold() should respect batch size via FETCH FIRST")
    void updateBatchStatusByStatusAndThreshold_respectsLockedBatchSize() {
        repository.saveBatch(
                IntStream.range(0, 5)
                        .mapToObj(i -> delegate.buildEvent(EventStatus.IN_PROCESS))
                        .toList()
        );

        Instant threshold = Instant.now().plusSeconds(1);
        int updated = repository.updateBatchStatusByStatusAndThreshold(
                EventStatus.IN_PROCESS, threshold, 3, EventStatus.PENDING
        );

        assertThat(updated).isEqualTo(3);
    }

    @Test
    @DisplayName("IT updateBatchStatusByStatusAndThreshold() when no matches should return zero")
    void updateBatchStatusByStatusAndThreshold_noMatches_returnsZero() {
        Instant pastThreshold = Instant.now().minusSeconds(3600);

        int updated = repository.updateBatchStatusByStatusAndThreshold(
                EventStatus.IN_PROCESS, pastThreshold, 10, EventStatus.PENDING
        );

        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("IT deleteBatchByStatusAndThreshold() should delete old processed events via SELECT + DELETE")
    void deleteBatchByStatusAndThreshold_deletesOldEvents() {
        repository.saveBatch(List.of(
                delegate.buildEvent(EventStatus.PROCESSED),
                delegate.buildEvent(EventStatus.PROCESSED)
        ));

        Instant threshold = Instant.now().plusSeconds(1);
        int deleted = repository.deleteBatchByStatusAndThreshold(
                EventStatus.PROCESSED, threshold, 10
        );

        assertThat(deleted).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("IT deleteBatchByStatusAndThreshold() should respect batch size via FETCH FIRST")
    void deleteBatchByStatusAndThreshold_respectsBatchSize() {
        repository.saveBatch(
                IntStream.range(0, 5)
                        .mapToObj(i -> delegate.buildEvent(EventStatus.PROCESSED))
                        .toList()
        );

        Instant threshold = Instant.now().plusSeconds(1);
        int deleted = repository.deleteBatchByStatusAndThreshold(
                EventStatus.PROCESSED, threshold, 3
        );

        assertThat(deleted).isEqualTo(3);
    }

    @Test
    @DisplayName("IT deleteBatchByStatusAndThreshold() when no matches should return zero")
    void deleteBatchByStatusAndThreshold_noMatches_returnsZero() {
        Instant pastThreshold = Instant.now().minusSeconds(3600);

        int deleted = repository.deleteBatchByStatusAndThreshold(
                EventStatus.PROCESSED, pastThreshold, 10
        );

        assertThat(deleted).isEqualTo(0);
    }
}