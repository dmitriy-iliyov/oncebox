package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInboxManagerUnitTests {

    @Mock
    private InboxRepository repository;

    @Mock
    private Clock clock;

    private DefaultInboxManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultInboxManager(repository, clock);
    }

    @Test
    @DisplayName("UT DefaultInboxManager() when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultInboxManager(null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repository cannot be null");
    }

    @Test
    @DisplayName("UT DefaultInboxManager() when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultInboxManager(repository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT tryConsume() when event not consumed should return true and save it")
    void tryConsume_whenEventNotConsumed_shouldReturnTrueAndSaveIt() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.saveIfAbsent(id)).thenReturn(1);

        // when
        boolean result = manager.tryConsume(id);

        // then
        assertThat(result).isTrue();
        verify(repository).saveIfAbsent(id);
    }

    @Test
    @DisplayName("UT tryConsume() when event already consumed should return false")
    void tryConsume_whenEventAlreadyConsumed_shouldReturnFalse() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.saveIfAbsent(id)).thenReturn(0);

        // when
        boolean result = manager.tryConsume(id);

        // then
        assertThat(result).isFalse();
        verify(repository).saveIfAbsent(id);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when no events consumed should return empty set")
    void tryConsumeAndGetDuplicates_whenNoEventsConsumed_shouldReturnEmptySet() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(id1, id2, id3));

        // when
        Set<UUID> result = manager.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).isEmpty();
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when all events consumed should return all ids")
    void tryConsumeAndGetDuplicates_whenAllEventsConsumed_shouldReturnAllIds() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of());

        // when
        Set<UUID> result = manager.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(id1, id2);
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() when some events consumed should return consumed ids")
    void tryConsumeAndGetDuplicates_whenSomeEventsConsumed_shouldReturnConsumedIds() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(id1));

        // when
        Set<UUID> result = manager.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(id2, id3);
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT tryConsumeAndGetDuplicates() with empty set should return empty set")
    void tryConsumeAndGetDuplicates_withEmptySet_shouldReturnEmptySet() {
        // given
        Set<UUID> ids = Set.of();
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of());

        // when
        Set<UUID> result = manager.tryConsumeAndGetDuplicates(ids);

        // then
        assertThat(result).isEmpty();
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() should calculate correct threshold and delegate")
    void cleanBatchByTtl_shouldCalculateCorrectThresholdAndDelegate() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(50);
        when(clock.instant()).thenReturn(Instant.now());

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(50);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant expectedThreshold = Instant.now().minusSeconds(ttl.toSeconds());
        assertThat(threshold).isBetween(
                expectedThreshold.minusSeconds(1),
                expectedThreshold.plusSeconds(1)
        );
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should return zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldReturnZero() {
        // given
        Duration ttl = Duration.ofHours(24);
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(0);
        when(clock.instant()).thenReturn(Instant.now());

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isZero();
        verify(repository).deleteBatchByThreshold(any(Instant.class), eq(batchSize));
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() with different ttl values should calculate correct threshold")
    void cleanBatchByTtl_withDifferentTtlValues_shouldCalculateCorrectThreshold() {
        // given
        Duration ttl = Duration.ofMinutes(30);
        int batchSize = 50;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(25);
        when(clock.instant()).thenReturn(Instant.now());

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(25);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant expectedThreshold = Instant.now().minusSeconds(1800);
        assertThat(threshold).isBetween(
                expectedThreshold.minusSeconds(1),
                expectedThreshold.plusSeconds(1)
        );
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() with zero ttl should use current time as threshold")
    void cleanBatchByTtl_withZeroTtl_shouldUseCurrentTimeAsThreshold() {
        // given
        Duration ttl = Duration.ZERO;
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(10);
        when(clock.instant()).thenReturn(Instant.now());

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(10);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant now = Instant.now();
        assertThat(threshold).isBetween(
                now.minusSeconds(1),
                now.plusSeconds(1)
        );
    }
}
