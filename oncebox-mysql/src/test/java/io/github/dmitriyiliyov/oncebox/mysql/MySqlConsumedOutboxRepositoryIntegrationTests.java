package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConcurrentInsertException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class MySqlConsumedOutboxRepositoryIntegrationTests extends BaseMySqlIntegrationTests {

    private final MySqlConsumedOutboxRepository repository;

    MySqlConsumedOutboxRepositoryIntegrationTests(
            @Qualifier("mysqlConsumedOutboxRepository") MySqlConsumedOutboxRepository repository
    ) {
        this.repository = repository;
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlConsumedOutboxRepository(null, repository.clock, repository.uuidHelper, repository.mapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlConsumedOutboxRepository(repository.jdbcTemplate, null, repository.uuidHelper, repository.mapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlConsumedOutboxRepository(repository.jdbcTemplate, repository.clock, null, repository.mapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlConsumedOutboxRepository(repository.jdbcTemplate, repository.clock, repository.uuidHelper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mapper cannot be null");
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) new id should return 1")
    void saveIfAbsent_newId_returnsOne() {
        int result = repository.saveIfAbsent(UUID.randomUUID());
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) duplicate id should return 0 via INSERT IGNORE")
    void saveIfAbsent_duplicateId_returnsZero() {
        UUID id = UUID.randomUUID();
        repository.saveIfAbsent(id);

        int result = repository.saveIfAbsent(id);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) multiple different ids should all return 1")
    void saveIfAbsent_multipleDifferentIds_eachReturnsOne() {
        assertThat(repository.saveIfAbsent(UUID.randomUUID())).isEqualTo(1);
        assertThat(repository.saveIfAbsent(UUID.randomUUID())).isEqualTo(1);
        assertThat(repository.saveIfAbsent(UUID.randomUUID())).isEqualTo(1);
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) new ids should return all inserted ids")
    void saveIfAbsent_newIds_returnsAllInserted() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        Set<UUID> inserted = repository.saveIfAbsent(ids);

        assertThat(inserted).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) duplicate ids should return only new ones")
    void saveIfAbsent_partiallyDuplicate_returnsOnlyNew() {
        UUID existing = UUID.randomUUID();
        repository.saveIfAbsent(existing);

        UUID newId = UUID.randomUUID();
        Set<UUID> inserted = repository.saveIfAbsent(Set.of(existing, newId));

        assertThat(inserted)
                .containsOnly(newId)
                .doesNotContain(existing);
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) all duplicates should return empty set")
    void saveIfAbsent_allDuplicates_returnsEmptySet() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        repository.saveIfAbsent(Set.of(id1, id2));

        Set<UUID> inserted = repository.saveIfAbsent(Set.of(id1, id2));

        assertThat(inserted).isEmpty();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) empty set should return empty set")
    void saveIfAbsent_emptySet_returnsEmptySet() {
        assertThat(repository.saveIfAbsent(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) large batch should persist all")
    void saveIfAbsent_largeBatch_persistsAll() {
        Set<UUID> ids = IntStream.range(0, 50)
                .mapToObj(i -> UUID.randomUUID())
                .collect(Collectors.toSet());

        Set<UUID> inserted = repository.saveIfAbsent(ids);

        assertThat(inserted)
                .hasSize(50)
                .containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) single new id in set returns singleton set")
    void saveIfAbsent_singleNewId_returnsSingletonSet() {
        UUID id = UUID.randomUUID();

        Set<UUID> inserted = repository.saveIfAbsent(Set.of(id));

        assertThat(inserted).containsOnly(id);
    }

    @Test
    @DisplayName("IT saveIfAbsent(UUID) second call for same id uses INSERT IGNORE and returns 0")
    void saveIfAbsent_insertIgnore_doesNotThrowOnDuplicate() {
        UUID id = UUID.randomUUID();
        repository.saveIfAbsent(Set.of(id));

        int result = repository.saveIfAbsent(id);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("IT saveIfAbsent(Set) throws ConcurrentInsertException when inserted rows mismatch")
    void saveIfAbsent_concurrentInsertMismatch_throwsException() {
        org.springframework.jdbc.core.JdbcTemplate interceptedJdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(repository.jdbcTemplate.getDataSource()) {
            @Override
            public int update(String sql, org.springframework.jdbc.core.PreparedStatementSetter pss) throws org.springframework.dao.DataAccessException {
                if (sql.contains("INSERT IGNORE INTO outbox_consumed_events")) {
                    return 0;
                }
                return super.update(sql, pss);
            }
        };

        MySqlConsumedOutboxRepository testRepository = new MySqlConsumedOutboxRepository(
                interceptedJdbcTemplate,
                repository.clock,
                repository.uuidHelper,
                repository.mapper
        );

        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> testRepository.saveIfAbsent(ids))
                .isInstanceOf(ConcurrentInsertException.class);
    }

    @Test
    @DisplayName("IT deleteBatchByThreshold() should delete old events via subquery")
    void deleteBatchByThreshold_deletesOldEvents() {
        repository.saveIfAbsent(UUID.randomUUID());
        repository.saveIfAbsent(UUID.randomUUID());

        Instant futureThreshold = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.MILLIS);
        int deleted = repository.deleteBatchByThreshold(futureThreshold, 10);

        assertThat(deleted).isEqualTo(2);
    }

    @Test
    @DisplayName("IT deleteBatchByThreshold() should not delete fresh events")
    void deleteBatchByThreshold_doesNotDeleteFreshEvents() {
        repository.saveIfAbsent(UUID.randomUUID());
        repository.saveIfAbsent(UUID.randomUUID());

        Instant pastThreshold = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        int deleted = repository.deleteBatchByThreshold(pastThreshold, 10);

        assertThat(deleted).isEqualTo(0);
    }

    @Test
    @DisplayName("IT deleteBatchByThreshold() should respect batch size via subquery LIMIT")
    void deleteBatchByThreshold_respectsBatchSize() {
        IntStream.range(0, 5).forEach(i -> repository.saveIfAbsent(UUID.randomUUID()));

        Instant futureThreshold = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.MILLIS);
        int deleted = repository.deleteBatchByThreshold(futureThreshold, 3);

        assertThat(deleted).isEqualTo(3);
    }

    @Test
    @DisplayName("IT deleteBatchByThreshold() when no events should return zero")
    void deleteBatchByThreshold_noEvents_returnsZero() {
        Instant futureThreshold = Instant.now().plusSeconds(10);
        int deleted = repository.deleteBatchByThreshold(futureThreshold, 10);
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    @DisplayName("IT deleteBatchByThreshold() should not affect events above threshold")
    void deleteBatchByThreshold_doesNotAffectEventsAboveThreshold() {
        UUID fresh = UUID.randomUUID();
        repository.saveIfAbsent(fresh);

        Instant pastThreshold = Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        repository.deleteBatchByThreshold(pastThreshold, 10);

        int result = repository.saveIfAbsent(fresh);
        assertThat(result).isEqualTo(0);
    }
}