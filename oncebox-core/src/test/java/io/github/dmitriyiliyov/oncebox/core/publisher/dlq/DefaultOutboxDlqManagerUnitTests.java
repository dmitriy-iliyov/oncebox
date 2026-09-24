package io.github.dmitriyiliyov.oncebox.core.publisher.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqManagerUnitTests {

    @Mock
    OutboxDlqRepository repository;

    @Mock
    Clock clock;

    @InjectMocks
    DefaultOutboxDlqManager tested;

    @Test
    @DisplayName("UT DefaultOutboxDlqManager() when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqManager(null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repository cannot be null");
    }

    @Test
    @DisplayName("UT DefaultOutboxDlqManager() when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqManager(repository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT saveBatch(), should early return if List.isEmpty")
    public void saveBatch_whenEventsIsEmpty_shouldEarlyReturn() {
        // given
        List<OutboxDlqEvent> events = List.of();

        // when
        tested.saveBatch(events);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT saveBatch() when arg is valid, should saveBatch")
    public void saveBatch_whenArgIsValid_shouldEarlyReturn() {
        // given
        List<OutboxDlqEvent> events = List.of(mock(OutboxDlqEvent.class));

        // when
        tested.saveBatch(events);

        // then
        verify(repository, times(1)).saveBatch(events);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids null should early return")
    void deleteBatch_whenIdsNull_shouldEarlyReturn() {
        // given
        Set<UUID> ids = null;

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(0, deleteCount);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids empty should early return")
    void deleteBatch_whenIdsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(0, deleteCount);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when all events valid, should delete batch")
    void deleteBatch_whenEventsValid_shouldDelete() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        when(repository.deleteBatch(ids)).thenReturn(ids.size());

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(ids.size(), deleteCount);
        verify(repository).deleteBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteProcessedBatch() when arguments valid should call repo")
    void deleteResolvedBatch_validArguments_calculatesThresholdAndCallsRepository() {
        Duration ttl = Duration.ofDays(7);
        int batchSize = 100;
        int expectedDeletedCount = 42;

        Instant now = Instant.now();
        Instant expectedThreshold = now.minusMillis(ttl.toMillis());
        when(repository.deleteBatchByStatusAndThreshold(
                eq(DlqStatus.RESOLVED),
                any(Instant.class),
                eq(batchSize)
        )).thenReturn(expectedDeletedCount);
        when(clock.instant()).thenReturn(now);

        int actualDeletedCount = tested.deleteResolvedBatch(ttl, batchSize);

        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);

        verify(repository).deleteBatchByStatusAndThreshold(
                eq(DlqStatus.RESOLVED),
                eq(expectedThreshold),
                eq(batchSize)
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteResolvedBatch() when ttl is null should throws")
    void deleteResolvedBatch_nullTtl_throwsNullPointerException() {
        int batchSize = 100;

        assertThatThrownBy(() -> tested.deleteResolvedBatch(null, batchSize))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ttl cannot be null");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT loadAndLockBatch() should lock events of the given DLQ status as IN_PROCESS")
    void loadAndLockBatch_shouldLockEventsOfStatusAsInProcess() {
        // given
        List<OutboxDlqEvent> locked = List.of(mock(OutboxDlqEvent.class));
        when(repository.findAndLockBatchByStatus(DlqStatus.TO_RETRY, 50, DlqStatus.IN_PROCESS)).thenReturn(locked);

        // when
        List<OutboxDlqEvent> result = tested.loadAndLockBatch(DlqStatus.TO_RETRY, 50);

        // then
        assertThat(result).isSameAs(locked);
    }
}
