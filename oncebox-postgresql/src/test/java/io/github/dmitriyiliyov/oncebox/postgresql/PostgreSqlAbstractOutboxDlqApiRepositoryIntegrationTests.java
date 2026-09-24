package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.dlq.api.MultiDialectOutboxDlqApiRepositoryVerifier;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PostgreSqlAbstractOutboxDlqApiRepositoryIntegrationTests extends BaseDlqApiPostgresSqlIntegrationTests {

    private final MultiDialectOutboxDlqApiRepositoryVerifier delegate;

    public PostgreSqlAbstractOutboxDlqApiRepositoryIntegrationTests(
            @Qualifier("postgresOutboxDlqApiRepository") OutboxDlqApiRepository repository,
            @Qualifier("postgresJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.delegate = new MultiDialectOutboxDlqApiRepositoryVerifier(
                repository,
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper()
        );
    }

    @Test
    @DisplayName("IT findById() for not existing id should return empty")
    void findById_notExisting_returnsEmpty() {
        delegate.findById_notExisting_returnsEmpty();
    }

    @Test
    @DisplayName("IT findByIdForUpdate() for not existing id should return empty")
    void findByIdForUpdate_notExisting_returnsEmpty() {
        delegate.findByIdForUpdate_notExisting_returnsEmpty();
    }

    @Test
    @DisplayName("IT findByIdForUpdate() for existing id should return event")
    void findByIdForUpdate_existingId_returnsEvent() {
        delegate.findByIdForUpdate_existingId_returnsEvent();
    }

    @Test
    @DisplayName("IT findBatch() when no matches should return empty")
    void findBatch_noMatches_returnsEmpty() {
        delegate.findBatch_noMatches_returnsEmpty();
    }

    @Test
    @DisplayName("IT findBatch() should be ordered by movedAt")
    void findBatch_orderedByMovedAt() {
        delegate.findBatch_orderedByMovedAt();
    }

    @Test
    @DisplayName("IT count() when table is empty should return 0")
    void count_emptyTable_returnsZero() {
        delegate.count_emptyTable_returnsZero();
    }

    @Test
    @DisplayName("IT count() when table has events should return total count")
    void count_withEvents_returnsTotalCount() {
        delegate.count_withEvents_returnsTotalCount();
    }

    @Test
    @DisplayName("IT count() by status when no matches should return 0")
    void count_byStatus_noMatches_returnsZero() {
        delegate.count_byStatus_noMatches_returnsZero();
    }

    @Test
    @DisplayName("IT count() by status when matches exist should return only matching count")
    void count_byStatus_withMatches_returnsOnlyMatchingCount() {
        delegate.count_byStatus_withMatches_returnsOnlyMatchingCount();
    }

    @Test
    @DisplayName("IT count() by eventType when no matches should return 0")
    void count_byEventType_noMatches_returnsZero() {
        delegate.count_byEventType_noMatches_returnsZero();
    }

    @Test
    @DisplayName("IT count() by eventType when matches exist should return only matching count")
    void count_byEventType_withMatches_returnsOnlyMatchingCount() {
        delegate.count_byEventType_withMatches_returnsOnlyMatchingCount();
    }

    @Test
    @DisplayName("IT count() by status and eventType when no matches should return 0")
    void count_byStatusAndEventType_noMatches_returnsZero() {
        delegate.count_byStatusAndEventType_noMatches_returnsZero();
    }

    @Test
    @DisplayName("IT count() by status and eventType when matches exist should return only matching count")
    void count_byStatusAndEventType_withMatches_returnsOnlyMatchingCount() {
        delegate.count_byStatusAndEventType_withMatches_returnsOnlyMatchingCount();
    }

    @Test
    @DisplayName("IT updateStatus() for existing event should change status")
    void updateStatus_existingEvent_statusChanged() {
        delegate.updateStatus_existingEvent_statusChanged();
    }

    @Test
    @DisplayName("IT updateStatus() for not existing id should not throw")
    void updateStatus_notExistingId_doesNotThrow() {
        delegate.updateStatus_notExistingId_doesNotThrow();
    }

    @Test
    @DisplayName("IT updateBatchStatus() for multiple ids should update all")
    void updateBatchStatus_multipleIds_allUpdated() {
        delegate.updateBatchStatus_multipleIds_allUpdated();
    }

    @Test
    @DisplayName("IT updateBatchStatus() should not affect other events")
    void updateBatchStatus_doesNotAffectOtherEvents() {
        delegate.updateBatchStatus_doesNotAffectOtherEvents();
    }

    @Test
    @DisplayName("IT updateBatchStatus() with empty ids should throw exception")
    void updateBatchStatus_emptyIds_throwsException() {
        delegate.updateBatchStatus_emptyIds_throwsException();
    }

    @Test
    @DisplayName("IT updateBatchStatus() by ids should skip events in IN_PROCESS status")
    void updateBatchStatus_skipsInProcess() {
        delegate.updateBatchStatus_skipsInProcess();
    }

    @Test
    @DisplayName("IT updateBatchStatus() by eventType should update only matching type")
    void updateBatchStatus_byEventType_updatesOnlyMatchingType() {
        delegate.updateBatchStatus_byEventType_updatesOnlyMatchingType();
    }

    @Test
    @DisplayName("IT updateBatchStatus() by eventType should skip events in IN_PROCESS status")
    void updateBatchStatus_byEventType_skipsInProcess() {
        delegate.updateBatchStatus_byEventType_skipsInProcess();
    }

    @Test
    @DisplayName("IT deleteById() for existing event should delete and return one")
    void deleteById_existingEvent_deletedAndReturnsOne() {
        delegate.deleteById_existingEvent_deletedAndReturnsOne();
    }

    @Test
    @DisplayName("IT deleteById() for not existing id should return zero")
    void deleteById_notExisting_returnsZero() {
        delegate.deleteById_notExisting_returnsZero();
    }

    @Test
    @DisplayName("IT deleteBatch() for existing ids should delete and return count")
    void deleteBatch_existingIds_deletedAndReturnsCount() {
        delegate.deleteBatch_existingIds_deletedAndReturnsCount();
    }

    @Test
    @DisplayName("IT deleteBatch() with empty ids should throw exception")
    void deleteBatch_emptyIds_throwsException() {
        delegate.deleteBatch_emptyIds_throwsException();
    }

    @Test
    @DisplayName("IT deleteBatch() should not affect other events")
    void deleteBatch_doesNotAffectOtherEvents() {
        delegate.deleteBatch_doesNotAffectOtherEvents();
    }

    @Test
    @DisplayName("IT deleteBatch() by ids should skip events in IN_PROCESS status")
    void deleteBatch_skipsInProcess() {
        delegate.deleteBatch_skipsInProcess();
    }

    @Test
    @DisplayName("IT deleteBatch() by eventType should delete only matching type")
    void deleteBatch_byEventType_deletesOnlyMatchingType() {
        delegate.deleteBatch_byEventType_deletesOnlyMatchingType();
    }

    @Test
    @DisplayName("IT deleteBatch() by eventType should skip events in IN_PROCESS status")
    void deleteBatch_byEventType_skipsInProcess() {
        delegate.deleteBatch_byEventType_skipsInProcess();
    }

    @Test
    @DisplayName("IT findById() for existing id should return event")
    void findById_existingId_returnsEvent() {
        delegate.findById_existingId_returnsEvent();
    }

    @Test
    @DisplayName("IT findBatch() by eventType should return only matching type")
    void findBatch_byEventType_returnsOnlyMatchingType() {
        delegate.findBatch_byEventType_returnsOnlyMatchingType();
    }

    @Test
    @DisplayName("IT findBatch() by status and eventType should return only matching events")
    void findBatch_byStatusAndEventType_returnsOnlyMatching() {
        delegate.findBatch_byStatusAndEventType_returnsOnlyMatching();
    }

    @Test
    @DisplayName("IT findBatch() without filters should return all events")
    void findBatch_noFilters_returnsAll() {
        delegate.findBatch_noFilters_returnsAll();
    }

    @Test
    @DisplayName("IT updateBatchStatus() by ids should return actual updated count")
    void updateBatchStatus_returnsActualUpdatedCount() {
        delegate.updateBatchStatus_returnsActualUpdatedCount();
    }

    @Test
    @DisplayName("IT updateBatchStatus() by eventType should return actual updated count")
    void updateBatchStatus_byEventType_returnsActualUpdatedCount() {
        delegate.updateBatchStatus_byEventType_returnsActualUpdatedCount();
    }

    @Test
    @DisplayName("IT updateBatchStatus() without ids and eventType should throw exception")
    void updateBatchStatus_neitherIdsNorEventType_throwsException() {
        delegate.updateBatchStatus_neitherIdsNorEventType_throwsException();
    }

    @Test
    @DisplayName("IT updateBatchStatus() without status should throw exception")
    void updateBatchStatus_withoutStatus_throwsException() {
        delegate.updateBatchStatus_withoutStatus_throwsException();
    }

    @Test
    @DisplayName("IT deleteBatch() by ids should return actual deleted count")
    void deleteBatch_returnsActualDeletedCount() {
        delegate.deleteBatch_returnsActualDeletedCount();
    }

    @Test
    @DisplayName("IT deleteBatch() by eventType should return actual deleted count")
    void deleteBatch_byEventType_returnsActualDeletedCount() {
        delegate.deleteBatch_byEventType_returnsActualDeletedCount();
    }

    @Test
    @DisplayName("IT deleteBatch() without ids and eventType should throw exception")
    void deleteBatch_neitherIdsNorEventType_throwsException() {
        delegate.deleteBatch_neitherIdsNorEventType_throwsException();
    }
}