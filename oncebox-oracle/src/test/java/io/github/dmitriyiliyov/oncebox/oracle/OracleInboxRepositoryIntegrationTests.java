package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.inbox.ConcurrentInsertException;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepositoryVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OracleInboxRepositoryIntegrationTests extends BaseOracleIntegrationTests {

    private static final UUID ID_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
    private static final UUID ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");

    private final OracleInboxRepository tested;
    private final InboxRepositoryVerifier delegate;

    OracleInboxRepositoryIntegrationTests(
            @Qualifier("oracleInboxRepository") OracleInboxRepository tested
    ) {
        this.tested = tested;
        this.delegate = new InboxRepositoryVerifier(tested, new OracleDatabaseClock(tested.jdbcTemplate));
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OracleInboxRepository(null, tested.uuidHelper, tested.mapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OracleInboxRepository(tested.jdbcTemplate, null, tested.mapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OracleInboxRepository(tested.jdbcTemplate, tested.uuidHelper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("mapper cannot be null");
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when another writer takes an id between the check and the insert should throw ConcurrentInsertException")
    void saveIfAbsentBatch_whenInsertedRowsMismatch_shouldThrowConcurrentInsertException() {
        // given
        JdbcTemplate losingJdbcTemplate = new JdbcTemplate(tested.jdbcTemplate.getDataSource()) {
            @Override
            public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
                                           ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
                return new int[][]{{0}};
            }
        };
        OracleInboxRepository racing = new OracleInboxRepository(losingJdbcTemplate, tested.uuidHelper, tested.mapper);

        // when / then
        assertThatThrownBy(() -> racing.saveIfAbsent(Set.of(ID_1, ID_2)))
                .isInstanceOf(ConcurrentInsertException.class);
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) when the id is new should return 1 and stamp consumed_at by the database clock")
    void saveIfAbsent_whenIdIsNew_shouldReturnOneAndStampConsumedAtByDatabaseClock() {
        delegate.saveIfAbsent_whenIdIsNew_shouldReturnOneAndStampConsumedAtByDatabaseClock();
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) when the id is already recorded should return 0")
    void saveIfAbsent_whenIdIsAlreadyRecorded_shouldReturnZero() {
        delegate.saveIfAbsent_whenIdIsAlreadyRecorded_shouldReturnZero();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when every id is new should return all of them and stamp consumed_at")
    void saveIfAbsentBatch_whenEveryIdIsNew_shouldReturnAllAndStampConsumedAt() {
        delegate.saveIfAbsentBatch_whenEveryIdIsNew_shouldReturnAllAndStampConsumedAt();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when some ids are recorded should return only the new ones")
    void saveIfAbsentBatch_whenSomeIdsAreRecorded_shouldReturnOnlyNewOnes() {
        delegate.saveIfAbsentBatch_whenSomeIdsAreRecorded_shouldReturnOnlyNewOnes();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when every id is recorded should return empty set")
    void saveIfAbsentBatch_whenEveryIdIsRecorded_shouldReturnEmptySet() {
        delegate.saveIfAbsentBatch_whenEveryIdIsRecorded_shouldReturnEmptySet();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when ids are empty should return empty set")
    void saveIfAbsentBatch_whenIdsAreEmpty_shouldReturnEmptySet() {
        delegate.saveIfAbsentBatch_whenIdsAreEmpty_shouldReturnEmptySet();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) when the batch is large should record every id")
    void saveIfAbsentBatch_whenBatchIsLarge_shouldRecordEveryId() {
        delegate.saveIfAbsentBatch_whenBatchIsLarge_shouldRecordEveryId();
    }

    @Test
    @DisplayName("IT deleteBatchOlderThan() should delete the records consumed longer than age ago and keep the fresh")
    void deleteBatchOlderThan_whenSomeRecordsAreOld_shouldDeleteOnlyThem() {
        delegate.deleteBatchOlderThan_whenSomeRecordsAreOld_shouldDeleteOnlyThem();
    }

    @Test
    @DisplayName("IT deleteBatchOlderThan() when every record is fresh should delete nothing")
    void deleteBatchOlderThan_whenEveryRecordIsFresh_shouldDeleteNothing() {
        delegate.deleteBatchOlderThan_whenEveryRecordIsFresh_shouldDeleteNothing();
    }

    @Test
    @DisplayName("IT deleteBatchOlderThan() should delete at most batchSize records")
    void deleteBatchOlderThan_whenMoreRecordsAreOld_shouldDeleteAtMostBatchSize() {
        delegate.deleteBatchOlderThan_whenMoreRecordsAreOld_shouldDeleteAtMostBatchSize();
    }

    @Test
    @DisplayName("IT deleteBatchOlderThan() when there are no records should return zero")
    void deleteBatchOlderThan_whenThereAreNoRecords_shouldReturnZero() {
        delegate.deleteBatchOlderThan_whenThereAreNoRecords_shouldReturnZero();
    }

    @Test
    @DisplayName("IT deleteBatchOlderThan() when the JVM runs in another time zone should still keep a fresh record")
    void deleteBatchOlderThan_whenJvmRunsInAnotherZone_shouldKeepFreshRecord() {
        delegate.deleteBatchOlderThan_whenJvmRunsInAnotherZone_shouldKeepFreshRecord();
    }
}
