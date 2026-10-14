package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepositoryVerifier;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PostgreSqlOutboxDlqRepositoryIntegrationTests extends BasePostgresSqlIntegrationTests {

    private final OutboxDlqRepository repository;
    private final OutboxDlqRepositoryVerifier delegate;

    private final JdbcTemplate jdbcTemplate;
    private final PostgreSqlUuidHelper postgreSqlUuidHelper = new PostgreSqlUuidHelper();
    private final DefaultResultSetMapper mapper = new DefaultResultSetMapper();

    public PostgreSqlOutboxDlqRepositoryIntegrationTests(
            @Qualifier("postgresOutboxDlqRepository") OutboxDlqRepository repository,
            @Qualifier("postgresJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.delegate = new OutboxDlqRepositoryVerifier(
                repository,
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper(),
                new PostgreSqlDatabaseClock(jdbcTemplate)
        );
    }

    @Test
    @DisplayName("IT saveBatch() with single event should persist correctly")
    void saveBatch_singleEvent_persistedCorrectly() {
        delegate.saveBatch_singleEvent_persistedCorrectly();
    }

    @Test
    @DisplayName("IT saveBatch() when an event is moved should keep its times and stamp moved_at by the database clock")
    void saveBatch_whenEventIsMoved_shouldKeepItsTimesAndStampMovedAtByDatabaseClock() {
        delegate.saveBatch_whenEventIsMoved_shouldKeepItsTimesAndStampMovedAtByDatabaseClock();
    }

    @Test
    @DisplayName("IT saveBatch() when read back in another JVM time zone should give back the same moments")
    void saveBatch_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoments() {
        delegate.saveBatch_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoments();
    }

    @Test
    @DisplayName("IT saveBatch() with multiple events should persist all")
    void saveBatch_multipleEvents_allPersisted() {
        delegate.saveBatch_multipleEvents_allPersisted();
    }

    @Test
    @DisplayName("IT deleteBatch() for existing ids should delete and return count")
    void deleteBatch_existingIds_deletedAndReturnsCount() {
        delegate.deleteBatch_existingIds_deletedAndReturnsCount();
    }

    @Test
    @DisplayName("IT deleteBatch() with empty ids should return zero")
    void deleteBatch_emptyIds_returnsZero() {
        delegate.deleteBatch_emptyIds_returnsZero();
    }

    @Test
    @DisplayName("IT deleteBatch() should not affect other events")
    void deleteBatch_doesNotAffectOtherEvents() {
        delegate.deleteBatch_doesNotAffectOtherEvents();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should lock and set status")
    void findAndLockBatchByStatus_locksAndSetsStatus() {
        OutboxDlqEvent e1 = delegate.buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = delegate.buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(e1, e2));

        List<OutboxDlqEvent> locked = repository
                .findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS);

        assertThat(locked)
                .hasSize(2)
                .extracting(OutboxDlqEvent::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());

        assertThat(delegate.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
        assertThat(delegate.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should respect batch size")
    void findAndLockBatchByStatus_respectsBatchSize() {
        repository.saveBatch(
                IntStream.range(0, 5)
                        .mapToObj(i -> delegate.buildEvent(DlqStatus.MOVED))
                        .toList()
        );

        assertThat(repository.findAndLockBatchByStatus(DlqStatus.MOVED, 3, DlqStatus.IN_PROCESS))
                .hasSize(3);
    }

    @Test
    @DisplayName("IT deleteBatchByStatusOlderThan() should delete matching events and return count")
    void deleteBatchByStatusOlderThan_matches_deleted() {
        delegate.deleteBatchByStatusOlderThan_matches_deleted();
    }

    @Test
    @DisplayName("IT deleteBatchByStatusOlderThan() should not delete events newer than threshold")
    void deleteBatchByStatusOlderThan_newerThanAge_notDeleted() {
        delegate.deleteBatchByStatusOlderThan_newerThanAge_notDeleted();
    }

    @Test
    @DisplayName("IT deleteBatchByStatusOlderThan() should not delete events with different status")
    void deleteBatchByStatusOlderThan_wrongStatus_notDeleted() {
        delegate.deleteBatchByStatusOlderThan_wrongStatus_notDeleted();
    }

    @Test
    @DisplayName("IT deleteBatchByStatusOlderThan() should respect batch size limit")
    void deleteBatchByStatusOlderThan_respectsBatchSize() {
        delegate.deleteBatchByStatusOlderThan_respectsBatchSize();
    }

    @Test
    @DisplayName("IT deleteBatchByStatusOlderThan() when no matches should return zero")
    void deleteBatchByStatusOlderThan_noMatches_returnsZero() {
        delegate.deleteBatchByStatusOlderThan_noMatches_returnsZero();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() when some events match should lock only them in the lock status")
    void findAndLockBatchByStatus_whenSomeEventsMatch_shouldLockOnlyThemInLockStatus() {
        delegate.findAndLockBatchByStatus_whenSomeEventsMatch_shouldLockOnlyThemInLockStatus();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() when more events match should lock at most batchSize")
    void findAndLockBatchByStatus_whenMoreEventsMatch_shouldLockAtMostBatchSize() {
        delegate.findAndLockBatchByStatus_whenMoreEventsMatch_shouldLockAtMostBatchSize();
    }
}
