package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInboxManagerUnitTests {

    private static final UUID ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ID_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID ID_3 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private DefaultInboxManager tested;

    @Test
    @DisplayName("UT constructor when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultInboxManager(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("repository cannot be null");
    }

    @Test
    @DisplayName("UT tryConsume() when the id is recorded by this call should return true")
    void tryConsume_whenIdIsRecordedByThisCall_shouldReturnTrue() {
        // given
        when(repository.saveIfAbsent(ID_1)).thenReturn(1);

        // when
        boolean result = tested.tryConsume(ID_1);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT tryConsume() when the id is already consumed should return false")
    void tryConsume_whenIdIsAlreadyConsumed_shouldReturnFalse() {
        // given
        when(repository.saveIfAbsent(ID_1)).thenReturn(0);

        // when
        boolean result = tested.tryConsume(ID_1);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when every id is recorded by this call should return empty set")
    void tryConsumeAndGetDuplicates_whenEveryIdIsRecorded_shouldReturnEmptySet() {
        // given
        Set<UUID> ids = Set.of(ID_1, ID_2, ID_3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(ID_1, ID_2, ID_3));

        // when
        Set<UUID> result = tested.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when every id is already consumed should return all of them")
    void tryConsumeAndGetDuplicates_whenEveryIdIsAlreadyConsumed_shouldReturnAllOfThem() {
        // given
        Set<UUID> ids = Set.of(ID_1, ID_2);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of());

        // when
        Set<UUID> result = tested.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when some ids are already consumed should return only those")
    void tryConsumeAndGetDuplicates_whenSomeIdsAreAlreadyConsumed_shouldReturnOnlyThose() {
        // given
        Set<UUID> ids = Set.of(ID_1, ID_2, ID_3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(ID_1));

        // when
        Set<UUID> result = tested.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(ID_2, ID_3);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when ids are empty should return empty set")
    void tryConsumeAndGetDuplicates_whenIdsAreEmpty_shouldReturnEmptySet() {
        // given
        when(repository.saveIfAbsent(Set.of())).thenReturn(Set.of());

        // when
        Set<UUID> result = tested.tryConsumeAndGetDuplicates(Set.of());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() should delete the records older than ttl and return their count")
    void cleanBatchByTtl_whenRecordsAreOlderThanTtl_shouldReturnDeletedCount() {
        // given
        Duration ttl = Duration.ofHours(1);
        when(repository.deleteBatchOlderThan(ttl, 100)).thenReturn(50);

        // when
        int result = tested.cleanBatchByTtl(ttl, 100);

        // then
        assertThat(result).isEqualTo(50);
        verify(repository).deleteBatchOlderThan(ttl, 100);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when nothing is old enough should return zero")
    void cleanBatchByTtl_whenNothingIsOldEnough_shouldReturnZero() {
        // given
        Duration ttl = Duration.ofDays(1);
        when(repository.deleteBatchOlderThan(ttl, 100)).thenReturn(0);

        // when
        int result = tested.cleanBatchByTtl(ttl, 100);

        // then
        assertThat(result).isZero();
    }
}
