package io.github.dmitriyiliyov.oncebox.core.inbox;

import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;

import java.time.Duration;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The contract of {@link InboxRepository} against the real engine, shared by the dialect modules: the times of the
 * inbox are set by the database, so a record is aged by moving its {@code consumed_at}.
 */
public class InboxRepositoryVerifier {

    private static final UUID ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ID_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID ID_3 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static final String TABLE = "inbox_events";
    private static final String COLUMN = "consumed_at";
    private static final Duration AGE = Duration.ofMinutes(30);
    private static final Duration LONG_AGO = Duration.ofHours(1).negated();
    private static final Duration SLACK = Duration.ofSeconds(30);

    private final InboxRepository repository;
    private final TestDatabaseClock clock;

    public InboxRepositoryVerifier(InboxRepository repository, TestDatabaseClock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void saveIfAbsent_whenIdIsNew_shouldReturnOneAndStampConsumedAtByDatabaseClock() {
        int result = repository.saveIfAbsent(ID_1);

        assertThat(result).isEqualTo(1);
        assertThat(clock.distanceFromNow(TABLE, COLUMN, ID_1)).isBetween(SLACK.negated(), SLACK);
    }

    public void saveIfAbsent_whenIdIsAlreadyRecorded_shouldReturnZero() {
        repository.saveIfAbsent(ID_1);

        int result = repository.saveIfAbsent(ID_1);

        assertThat(result).isZero();
    }

    public void saveIfAbsentBatch_whenEveryIdIsNew_shouldReturnAllAndStampConsumedAt() {
        Set<UUID> inserted = repository.saveIfAbsent(Set.of(ID_1, ID_2, ID_3));

        assertThat(inserted).containsExactlyInAnyOrder(ID_1, ID_2, ID_3);
        assertThat(clock.distanceFromNow(TABLE, COLUMN, ID_2)).isBetween(SLACK.negated(), SLACK);
    }

    public void saveIfAbsentBatch_whenSomeIdsAreRecorded_shouldReturnOnlyNewOnes() {
        repository.saveIfAbsent(ID_1);

        Set<UUID> inserted = repository.saveIfAbsent(Set.of(ID_1, ID_2));

        assertThat(inserted).containsExactly(ID_2);
    }

    public void saveIfAbsentBatch_whenEveryIdIsRecorded_shouldReturnEmptySet() {
        repository.saveIfAbsent(Set.of(ID_1, ID_2));

        Set<UUID> inserted = repository.saveIfAbsent(Set.of(ID_1, ID_2));

        assertThat(inserted).isEmpty();
    }

    public void saveIfAbsentBatch_whenIdsAreEmpty_shouldReturnEmptySet() {
        assertThat(repository.saveIfAbsent(Set.of())).isEmpty();
    }

    public void saveIfAbsentBatch_whenIdsAreNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> repository.saveIfAbsent((Set<UUID>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ids cannot be null");
    }

    public void saveIfAbsentBatch_whenBatchIsLarge_shouldRecordEveryId() {
        Set<UUID> ids = IntStream.range(0, 50).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toSet());

        Set<UUID> inserted = repository.saveIfAbsent(ids);

        assertThat(inserted).containsExactlyInAnyOrderElementsOf(ids);
    }

    public void deleteBatchOlderThan_whenSomeRecordsAreOld_shouldDeleteOnlyThem() {
        repository.saveIfAbsent(Set.of(ID_1, ID_2, ID_3));
        clock.shift(TABLE, COLUMN, ID_1, LONG_AGO);
        clock.shift(TABLE, COLUMN, ID_2, LONG_AGO);

        int deleted = repository.deleteBatchOlderThan(AGE, 10);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.saveIfAbsent(Set.of(ID_1, ID_2, ID_3))).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    public void deleteBatchOlderThan_whenEveryRecordIsFresh_shouldDeleteNothing() {
        repository.saveIfAbsent(Set.of(ID_1, ID_2));

        assertThat(repository.deleteBatchOlderThan(AGE, 10)).isZero();
    }

    public void deleteBatchOlderThan_whenMoreRecordsAreOld_shouldDeleteAtMostBatchSize() {
        Set<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toSet());
        repository.saveIfAbsent(ids);
        ids.forEach(id -> clock.shift(TABLE, COLUMN, id, LONG_AGO));

        assertThat(repository.deleteBatchOlderThan(AGE, 3)).isEqualTo(3);
    }

    public void deleteBatchOlderThan_whenThereAreNoRecords_shouldReturnZero() {
        assertThat(repository.deleteBatchOlderThan(AGE, 10)).isZero();
    }

    public void deleteBatchOlderThan_whenJvmRunsInAnotherZone_shouldKeepFreshRecord() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            repository.saveIfAbsent(ID_1);
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));

            int deleted = repository.deleteBatchOlderThan(AGE, 10);

            assertThat(deleted).isZero();
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
