package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqManagerUnitTests {

    @Mock
    OutboxDlqRepository repository;

    @InjectMocks
    DefaultOutboxDlqManager tested;

    @Test
    @DisplayName("UT DefaultOutboxDlqManager() when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqManager(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repository cannot be null");
    }

    @Test
    @DisplayName("UT saveBatch() when events is null should save nothing")
    void saveBatch_whenEventsIsNull_shouldSaveNothing() {
        // when
        tested.saveBatch(null);

        // then
        verifyNoInteractions(repository);
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
        assertThat(deleteCount).isEqualTo(0);
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
        assertThat(deleteCount).isEqualTo(0);
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
        assertThat(deleteCount).isEqualTo(ids.size());
        verify(repository).deleteBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteResolvedBatch() should delete RESOLVED events older than ttl")
    void deleteResolvedBatch_whenEventsAreOlderThanTtl_shouldDeleteThem() {
        // given
        Duration ttl = Duration.ofDays(7);
        when(repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, ttl, 100)).thenReturn(42);

        // when
        int deleted = tested.deleteResolvedBatch(ttl, 100);

        // then
        assertThat(deleted).isEqualTo(42);
        verify(repository).deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, ttl, 100);
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
