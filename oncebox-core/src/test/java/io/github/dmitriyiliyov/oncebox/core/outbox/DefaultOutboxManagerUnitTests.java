package io.github.dmitriyiliyov.oncebox.core.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxManagerUnitTests {

    private static final UUID LOCK_TOKEN = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ID_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID ID_3 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static final Instant NEXT_RETRY_AT = Instant.parse("2026-01-01T10:05:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration DELAY = Duration.ofSeconds(30);
    private static final Function<Integer, Duration> FIXED_DELAY = retryCount -> DELAY;

    @Mock
    private OutboxRepository repository;

    @InjectMocks
    private DefaultOutboxManager tested;

    @Test
    @DisplayName("UT constructor when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxManager(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("repository cannot be null");
    }

    @Test
    @DisplayName("UT save() should hand the event to the repository")
    void save_whenCalled_shouldHandEventToRepository() {
        // given
        OutboxEvent event = event(ID_1, 0);

        // when
        tested.save(event);

        // then
        verify(repository).save(event);
    }

    @Test
    @DisplayName("UT saveBatch() should hand the batch to the repository")
    void saveBatch_whenBatchIsNotEmpty_shouldHandBatchToRepository() {
        // given
        List<OutboxEvent> batch = List.of(event(ID_1, 0));

        // when
        tested.saveBatch(batch);

        // then
        verify(repository).saveBatch(batch);
    }

    @Test
    @DisplayName("UT saveBatch() when the batch is null should save nothing")
    void saveBatch_whenBatchIsNull_shouldSaveNothing() {
        // when
        tested.saveBatch(null);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT saveBatch() when the batch is empty should save nothing")
    void saveBatch_whenBatchIsEmpty_shouldSaveNothing() {
        // when
        tested.saveBatch(List.of());

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT saveBatchWithCreatedAt() should hand the batch to the repository")
    void saveBatchWithCreatedAt_whenBatchIsNotEmpty_shouldHandBatchToRepository() {
        // given
        List<OutboxEvent> batch = List.of(event(ID_1, 0));

        // when
        tested.saveBatchWithCreatedAt(batch);

        // then
        verify(repository).saveBatchWithCreatedAt(batch);
    }

    @Test
    @DisplayName("UT saveBatchWithCreatedAt() when the batch is null should save nothing")
    void saveBatchWithCreatedAt_whenBatchIsNull_shouldSaveNothing() {
        // when
        tested.saveBatchWithCreatedAt(null);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT saveBatchWithCreatedAt() when the batch is empty should save nothing")
    void saveBatchWithCreatedAt_whenBatchIsEmpty_shouldSaveNothing() {
        // when
        tested.saveBatchWithCreatedAt(List.of());

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(String) should capture due PENDING events as IN_PROCESS under the lock token")
    void loadBatchByType_whenEventsAreDue_shouldCaptureThemUnderLockToken() {
        // given
        List<OutboxEvent> captured = List.of(event(ID_1, 0), event(ID_2, 0));
        when(repository.findAndLockBatchByEventTypeAndStatus("type", EventStatus.PENDING, 10, LOCK_TOKEN,
                EventStatus.IN_PROCESS)).thenReturn(captured);

        // when
        List<OutboxEvent> result = tested.loadBatch("type", 10, LOCK_TOKEN);

        // then
        assertThat(result).isEqualTo(captured);
    }

    @Test
    @DisplayName("UT loadBatch(String) when nothing is due should return empty list")
    void loadBatchByType_whenNothingIsDue_shouldReturnEmptyList() {
        // given
        when(repository.findAndLockBatchByEventTypeAndStatus("type", EventStatus.PENDING, 10, LOCK_TOKEN,
                EventStatus.IN_PROCESS)).thenReturn(List.of());

        // when
        List<OutboxEvent> result = tested.loadBatch("type", 10, LOCK_TOKEN);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT loadBatch(String) when lockToken is null should throw and capture nothing")
    void loadBatchByType_whenLockTokenIsNull_shouldThrowAndCaptureNothing() {
        // when / then
        assertThatThrownBy(() -> tested.loadBatch("type", 10, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lockToken cannot be null");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(EventStatus) should capture the events of that status as IN_PROCESS")
    void loadBatchByStatus_whenEventsHaveStatus_shouldCaptureThemAsInProcess() {
        // given
        List<OutboxEvent> captured = List.of(event(ID_1, 3));
        when(repository.findAndLockBatchByStatus(EventStatus.FAILED, 10, EventStatus.IN_PROCESS)).thenReturn(captured);

        // when
        List<OutboxEvent> result = tested.loadBatch(EventStatus.FAILED, 10);

        // then
        assertThat(result).isEqualTo(captured);
    }

    @Test
    @DisplayName("UT finalizeBatch() when lockToken is null should throw and write nothing")
    void finalizeBatch_whenLockTokenIsNull_shouldThrowAndWriteNothing() {
        // when / then
        assertThatThrownBy(() -> tested.finalizeBatch(List.of(), Set.of(ID_1), null, 3, FIXED_DELAY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lockToken cannot be null");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when maxRetryCount is negative should throw and write nothing")
    void finalizeBatch_whenMaxRetryCountIsNegative_shouldThrowAndWriteNothing() {
        // when / then
        assertThatThrownBy(() -> tested.finalizeBatch(List.of(), null, Set.of(ID_1), -1, FIXED_DELAY, LOCK_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRetryCount cannot be negative");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when both id sets are empty should write nothing")
    void finalizeBatch_whenBothIdSetsAreEmpty_shouldWriteNothing() {
        // when
        tested.finalizeBatch(List.of(), Set.of(), Set.of(), 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when only processed ids are given should mark them PROCESSED under the token")
    void finalizeBatch_whenOnlyProcessedIds_shouldMarkThemProcessedUnderToken() {
        // when
        tested.finalizeBatch(List.of(event(ID_1, 0)), Set.of(ID_1), null, 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        verify(repository).updateBatchStatusByLockToken(Set.of(ID_1), LOCK_TOKEN, EventStatus.PROCESSED);
        verify(repository, never()).partiallyUpdateBatchByLockToken(any(), any());
    }

    @Test
    @DisplayName("UT finalizeBatch() when only failed ids are given should write only their retry state")
    void finalizeBatch_whenOnlyFailedIds_shouldWriteOnlyRetryState() {
        // when
        tested.finalizeBatch(List.of(event(ID_1, 0)), Set.of(), Set.of(ID_1), 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        verify(repository).partiallyUpdateBatchByLockToken(any(), eq(LOCK_TOKEN));
        verify(repository, never()).updateBatchStatusByLockToken(any(), any(), any());
    }

    @Test
    @DisplayName("UT finalizeBatch() when a failed event has retries left should make it PENDING with the supplied delay")
    void finalizeBatch_whenFailedEventHasRetriesLeft_shouldMakeItPendingWithSuppliedDelay() {
        // given
        List<Integer> askedFor = new ArrayList<>();
        Function<Integer, Duration> delays = retryCount -> {
            askedFor.add(retryCount);
            return DELAY;
        };

        // when
        tested.finalizeBatch(List.of(event(ID_1, 1)), null, Set.of(ID_1), 3, delays, LOCK_TOKEN);

        // then
        OutboxEvent written = writtenFailedEvents().getFirst();
        assertThat(written.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(written.getRetryCount()).isEqualTo(2);
        assertThat(written.getRetryDelay()).isEqualTo(DELAY);
        assertThat(askedFor).containsExactly(2);
    }

    @Test
    @DisplayName("UT finalizeBatch() when a failed event runs out of retries should make it FAILED with no delay")
    void finalizeBatch_whenFailedEventRunsOutOfRetries_shouldMakeItFailedWithNoDelay() {
        // when
        tested.finalizeBatch(List.of(event(ID_1, 2)), null, Set.of(ID_1), 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        OutboxEvent written = writtenFailedEvents().getFirst();
        assertThat(written.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(written.getRetryCount()).isEqualTo(3);
        assertThat(written.getRetryDelay()).isNull();
    }

    @Test
    @DisplayName("UT finalizeBatch() when maxRetryCount is zero should fail the event at once without asking for a delay")
    void finalizeBatch_whenMaxRetryCountIsZero_shouldFailEventWithoutAskingForDelay() {
        // given
        Function<Integer, Duration> delays = retryCount -> {
            throw new AssertionError("no delay is needed for an event out of retries");
        };

        // when
        tested.finalizeBatch(List.of(event(ID_1, 0)), null, Set.of(ID_1), 0, delays, LOCK_TOKEN);

        // then
        OutboxEvent written = writtenFailedEvents().getFirst();
        assertThat(written.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(written.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("UT finalizeBatch() when an id is both processed and failed should treat it as failed")
    void finalizeBatch_whenIdIsBothProcessedAndFailed_shouldTreatItAsFailed() {
        // given
        List<OutboxEvent> events = List.of(event(ID_1, 0), event(ID_2, 0), event(ID_3, 0));

        // when
        tested.finalizeBatch(events, Set.of(ID_1, ID_2), Set.of(ID_1, ID_3), 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        verify(repository).updateBatchStatusByLockToken(Set.of(ID_2), LOCK_TOKEN, EventStatus.PROCESSED);
        assertThat(writtenFailedEvents()).extracting(OutboxEvent::getId).containsExactlyInAnyOrder(ID_1, ID_3);
    }

    @Test
    @DisplayName("UT finalizeBatch() when every processed id is also failed should mark nothing PROCESSED")
    void finalizeBatch_whenEveryProcessedIdIsAlsoFailed_shouldMarkNothingProcessed() {
        // when
        tested.finalizeBatch(List.of(event(ID_1, 0)), Set.of(ID_1), Set.of(ID_1), 3, FIXED_DELAY, LOCK_TOKEN);

        // then
        verify(repository, never()).updateBatchStatusByLockToken(any(), any(), any());
        assertThat(writtenFailedEvents()).extracting(OutboxEvent::getId).containsExactly(ID_1);
    }

    @Test
    @DisplayName("UT recoverStuckBatch() should hand IN_PROCESS events older than the processing time back to PENDING")
    void recoverStuckBatch_whenEventsAreStuck_shouldHandThemBackToPending() {
        // given
        Duration maxProcessingTime = Duration.ofMinutes(15);
        when(repository.updateBatchStatusByStatusOlderThan(EventStatus.IN_PROCESS, maxProcessingTime, 50,
                EventStatus.PENDING)).thenReturn(12);

        // when
        int recovered = tested.recoverStuckBatch(maxProcessingTime, 50);

        // then
        assertThat(recovered).isEqualTo(12);
    }

    @Test
    @DisplayName("UT deleteProcessedBatch() should delete PROCESSED events older than ttl")
    void deleteProcessedBatch_whenEventsAreOlderThanTtl_shouldDeleteThem() {
        // given
        Duration ttl = Duration.ofHours(24);
        when(repository.deleteBatchByStatusOlderThan(EventStatus.PROCESSED, ttl, 100)).thenReturn(7);

        // when
        int deleted = tested.deleteProcessedBatch(ttl, 100);

        // then
        assertThat(deleted).isEqualTo(7);
    }

    @Test
    @DisplayName("UT deleteBatch() should delete the events with the given ids")
    void deleteBatch_whenIdsAreGiven_shouldDeleteThem() {
        // given
        when(repository.deleteBatch(Set.of(ID_1, ID_2))).thenReturn(2);

        // when
        int deleted = tested.deleteBatch(Set.of(ID_1, ID_2));

        // then
        assertThat(deleted).isEqualTo(2);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids are null should delete nothing and return zero")
    void deleteBatch_whenIdsAreNull_shouldDeleteNothing() {
        // when
        int deleted = tested.deleteBatch(null);

        // then
        assertThat(deleted).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids are empty should delete nothing and return zero")
    void deleteBatch_whenIdsAreEmpty_shouldDeleteNothing() {
        // when
        int deleted = tested.deleteBatch(Set.of());

        // then
        assertThat(deleted).isZero();
        verifyNoInteractions(repository);
    }

    @SuppressWarnings("unchecked")
    private List<OutboxEvent> writtenFailedEvents() {
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).partiallyUpdateBatchByLockToken(captor.capture(), eq(LOCK_TOKEN));
        verifyNoMoreInteractions(repository);
        return captor.getValue();
    }

    private static OutboxEvent event(UUID id, int retryCount) {
        return new OutboxEvent(id, EventStatus.IN_PROCESS, "type", "payloadType", "payload", retryCount,
                NEXT_RETRY_AT, CREATED_AT, CREATED_AT);
    }
}
