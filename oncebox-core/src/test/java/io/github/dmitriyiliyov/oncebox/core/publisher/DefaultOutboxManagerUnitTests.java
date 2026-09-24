package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultOutboxManagerUnitTests {

    @Mock
    OutboxRepository repository;

    @Mock
    Clock clock;

    @InjectMocks
    DefaultOutboxManager tested;

    @Test
    @DisplayName("UT constructor when repository is null should throw NullPointerException")
    void constructor_whenRepositoryIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxManager(null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repository cannot be null");
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxManager(repository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT loadBatch(String, int) when events is not empty, should update status and return event list")
    public void loadBatch_whenEventsIsNotEmpty_shouldUpdateStatusAndReturnEvents() {
        // given
        String eventType = "event-type";
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> eventMocks = List.of(event1, event2);

        when(repository.findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus))
                .thenReturn(eventMocks);

        // when
        List<OutboxEvent> result = tested.loadBatch(eventType, batchSize);

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly(event1, event2);

        verify(repository, times(1))
                .findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(String, int) when events is empty, should not update status and return empty event list")
    public void loadBatch_whenEventsIsEmpty_shouldReturnEmptyList() {
        // given
        String eventType = "event-type";
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        when(repository.findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus))
                .thenReturn(List.of());

        // when
        List<OutboxEvent> result = tested.loadBatch(eventType, batchSize);

        // then
        assertTrue(result.isEmpty());

        verify(repository, times(1))
                .findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(EventStatus, int, String) when events is not empty, should update status and return event list")
    public void loadBatch2_whenEventsIsNotEmpty_shouldUpdateStatusAndReturnEvents() {
        // given
        EventStatus status = EventStatus.FAILED;
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> eventMocks = List.of(event1, event2);

        when(repository.findAndLockBatchByStatus(status, batchSize, lockStatus)).thenReturn(eventMocks);

        // when
        List<OutboxEvent> result = tested.loadBatch(status, batchSize);

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly(event1, event2);

        verify(repository, times(1)).findAndLockBatchByStatus(status, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(EventStatus, int, String) when events is empty, should not update status and return empty event list")
    public void loadBatch2_whenEventsIsEmpty_shouldReturnEmptyList() {
        // given
        EventStatus status = EventStatus.FAILED;
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        when(repository.findAndLockBatchByStatus(status, batchSize, lockStatus)).thenReturn(List.of());

        // when
        List<OutboxEvent> result = tested.loadBatch(status, batchSize);

        // then
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAndLockBatchByStatus(status, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed and failed ids are non-empty")
    public void finalizeBatch_whenProcessedAndFailed_shouldCallCorrectRepositoryMethods() {
        // given
        UUID idProcessed = UUID.randomUUID();
        UUID idFailed = UUID.randomUUID();
        OutboxEvent failedEvent = new OutboxEvent(idFailed, EventStatus.PENDING, "type", "payloadType",
                "payload", 0, Instant.now(), Instant.now(), Instant.now());
        List<OutboxEvent> events = List.of(failedEvent);
        Set<UUID> processedIds = new HashSet<>(Set.of(idProcessed));
        Set<UUID> failedIds = new HashSet<>(Set.of(idFailed));

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 3, i -> Instant.now().plusSeconds(60));

        // then
        verify(repository, times(1)).updateBatchStatus(processedIds, EventStatus.PROCESSED);

        ArgumentCaptor<List<OutboxEvent>> failedCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).partiallyUpdateBatch(failedCaptor.capture());
        assertThat(failedCaptor.getValue()).hasSize(1);
        assertThat(failedCaptor.getValue().get(0).getId()).isEqualTo(idFailed);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed ids is null")
    public void finalizeBatch_whenProcessedIdsNull_shouldProcessFailedOnly() {
        // given
        UUID idFailed = UUID.randomUUID();
        OutboxEvent failedEvent = new OutboxEvent(idFailed, EventStatus.PENDING, "type", "payloadType",
                "payload", 0, Instant.now(), Instant.now(), Instant.now());
        List<OutboxEvent> events = List.of(failedEvent);

        // when
        tested.finalizeBatch(events, null, Set.of(idFailed), 2, i -> Instant.now());

        // then
        verify(repository, never()).updateBatchStatus(any(), any());
        verify(repository, times(1)).partiallyUpdateBatch(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed ids is empty")
    public void finalizeBatch_whenProcessedIdsEmpty_shouldProcessFailedOnly() {
        // given
        UUID idFailed = UUID.randomUUID();
        OutboxEvent failedEvent = new OutboxEvent(idFailed, EventStatus.PENDING, "type", "payloadType",
                "payload", 0, Instant.now(), Instant.now(), Instant.now());
        List<OutboxEvent> events = List.of(failedEvent);

        // when
        tested.finalizeBatch(events, Set.of(), Set.of(idFailed), 2, i -> Instant.now());

        // then
        verify(repository, never()).updateBatchStatus(any(), any());
        verify(repository, times(1)).partiallyUpdateBatch(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when failed ids is null")
    public void finalizeBatch_whenFailedIdsNull_shouldProcessProcessedOnly() {
        // given
        UUID idProcessed = UUID.randomUUID();
        Set<UUID> processedIds = Set.of(idProcessed);

        // when
        tested.finalizeBatch(List.of(), processedIds, null, 2, i -> Instant.now());

        // then
        verify(repository, times(1)).updateBatchStatus(processedIds, EventStatus.PROCESSED);
        verify(repository, never()).partiallyUpdateBatch(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when both processedIds and failedIds empty should not call repository")
    public void finalizeBatch_whenBothEmpty_shouldNotCallRepository() {
        // given / when
        tested.finalizeBatch(List.of(), Collections.emptySet(), Collections.emptySet(), 1, i -> Instant.now());

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processedIds and failedIds overlap should remove overlap from processed")
    public void finalizeBatch_whenOverlap_shouldRemoveOverlapFromProcessed() {
        // given
        UUID common = UUID.randomUUID();
        UUID idProcessed = UUID.randomUUID();
        UUID idFailed = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common, idProcessed));
        Set<UUID> failedIds = new HashSet<>(Set.of(common, idFailed));
        List<OutboxEvent> events = List.of(
                new OutboxEvent(common, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idProcessed, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 0, Instant.now(), Instant.now(), Instant.now())
        );

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 3, i -> Instant.now());

        // then
        verify(repository, times(1)).updateBatchStatus(Set.of(idProcessed), EventStatus.PROCESSED);
        ArgumentCaptor<List<OutboxEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).partiallyUpdateBatch(eventsCaptor.capture());
        assertThat(eventsCaptor.getValue()).containsExactlyInAnyOrder(
                new OutboxEvent(common, EventStatus.PENDING, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed, EventStatus.PENDING, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now())
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processedIds after overlap resolving is empty")
    public void finalizeBatch_whenProcessedIdsIsEmptyAfterOverlapResolve_shouldNotInvolveRepository() {
        // given
        UUID common = UUID.randomUUID();
        UUID idFailed = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common));
        Set<UUID> failedIds = new HashSet<>(Set.of(common, idFailed));
        List<OutboxEvent> events = List.of(
                new OutboxEvent(common, EventStatus.IN_PROCESS, "type", "payloadType", "payload", 0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed, EventStatus.IN_PROCESS, "type", "payloadType", "payload", 0, Instant.now(), Instant.now(), Instant.now())
        );

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 3, i -> Instant.now());

        // then
        verify(repository, never()).updateBatchStatus(any(Set.class), any(EventStatus.class));
        verify(repository, times(1)).partiallyUpdateBatch(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT prepareFailedEvents() when retry count exceeded threshold should set FAILED as new status")
    public void prepareFailedEvents_whenRetryCountExceededThreshold_shouldSetFailed() {
        // given
        UUID common = UUID.randomUUID();
        UUID idProcessed = UUID.randomUUID();
        UUID idFailed1 = UUID.randomUUID();
        UUID idFailed2 = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common, idProcessed));
        Set<UUID> failedIds = new HashSet<>(Set.of(common, idFailed1, idFailed2));
        List<OutboxEvent> events = List.of(
                new OutboxEvent(common, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idProcessed, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload",  0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed1, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed2, EventStatus.IN_PROCESS, "failed-event", "payloadType",
                        "payload", 2, Instant.now(), Instant.now(), Instant.now())
        );

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 3, i -> Instant.now());

        // then
        verify(repository, times(1)).updateBatchStatus(any(Set.class), any(EventStatus.class));
        ArgumentCaptor<List<OutboxEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).partiallyUpdateBatch(eventsCaptor.capture());
        List<OutboxEvent> resultEvents = eventsCaptor.getValue();
        assertEquals(3, resultEvents.size());
        for (OutboxEvent event: resultEvents) {
            if (event.getId().equals(idFailed2)) {
                assertEquals("failed-event", event.getEventType());
                assertEquals(EventStatus.FAILED, event.getStatus());
            } else {
                assertEquals(EventStatus.PENDING, event.getStatus());
            }
        }
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT prepareFailedEvents() when retry count not exceeded threshold should set FAILED as new status")
    public void prepareFailedEvents_whenRetryCountNotExceededThreshold_shouldSetNotFailed() {
        // given
        UUID common = UUID.randomUUID();
        UUID idProcessed = UUID.randomUUID();
        UUID idFailed1 = UUID.randomUUID();
        UUID idFailed2 = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common, idProcessed));
        Set<UUID> failedIds = new HashSet<>(Set.of(common, idFailed1, idFailed2));
        List<OutboxEvent> events = List.of(
                new OutboxEvent(common, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idProcessed, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload",  0, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed1, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now()),
                new OutboxEvent(idFailed2, EventStatus.IN_PROCESS, "type", "payloadType",
                        "payload", 1, Instant.now(), Instant.now(), Instant.now())
        );

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 3, i -> Instant.now());

        // then
        ArgumentCaptor<Set<UUID>> processedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1)).updateBatchStatus(processedIdsCaptor.capture(), eq(EventStatus.PROCESSED));
        assertFalse(processedIdsCaptor.getValue().contains(common));

        ArgumentCaptor<List<OutboxEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).partiallyUpdateBatch(eventsCaptor.capture());
        List<OutboxEvent> resultEvents = eventsCaptor.getValue();
        for (OutboxEvent event: resultEvents) {
            assertEquals(EventStatus.PENDING, event.getStatus());
        }
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("recoverStuckBatch() with valid arguments should calculate threshold and recover events")
    void recoverStuckBatch_validArguments_recoversEvents() {
        Duration maxProcessingTime = Duration.ofMinutes(15);
        int batchSize = 50;
        int expectedRecoveredCount = 12;
        Instant now = Instant.now();
        Instant expectedThreshold = now.minusMillis(maxProcessingTime.toMillis());

        when(repository.updateBatchStatusByStatusAndThreshold(
                eq(EventStatus.IN_PROCESS),
                any(Instant.class),
                eq(batchSize),
                eq(EventStatus.PENDING)
        )).thenReturn(expectedRecoveredCount);
        when(clock.instant()).thenReturn(now);

        int actualCount = tested.recoverStuckBatch(maxProcessingTime, batchSize);

        assertThat(actualCount).isEqualTo(expectedRecoveredCount);

        verify(repository).updateBatchStatusByStatusAndThreshold(
                eq(EventStatus.IN_PROCESS),
                eq(expectedThreshold),
                eq(batchSize),
                eq(EventStatus.PENDING)
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("recoverStuckBatch() with null maxBatchProcessingTime should throw NullPointerException")
    void recoverStuckBatch_nullDuration_throwsNullPointerException() {
        int batchSize = 50;

        assertThatThrownBy(() -> tested.recoverStuckBatch(null, batchSize))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids not null and not empty should delete")
    public void delete_whenIdsValid_shouldDelete() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        // when
        tested.deleteBatch(ids);

        // then
        ArgumentCaptor<Set<UUID>> idsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1)).deleteBatch(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(id1, id2);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids null should not delete")
    public void delete_whenIdsIsNull_shouldNotDelete() {
        // given
        Set<UUID> ids = null;

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should not delete")
    public void delete_whenIdsIsEmpty_shouldNotDelete() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT save() should hand the event to the repository")
    void save_shouldDelegateToRepository() {
        // given
        OutboxEvent event = mock(OutboxEvent.class);

        // when
        tested.save(event);

        // then
        verify(repository).save(event);
    }

    @Test
    @DisplayName("UT saveBatch() should hand the batch to the repository")
    void saveBatch_shouldDelegateToRepository() {
        // given
        List<OutboxEvent> batch = List.of(mock(OutboxEvent.class));

        // when
        tested.saveBatch(batch);

        // then
        verify(repository).saveBatch(batch);
    }

    @Test
    @DisplayName("UT deleteProcessedBatch() should delete PROCESSED events updated before now minus ttl")
    void deleteProcessedBatch_shouldDeleteProcessedOlderThanTtl() {
        // given
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(repository.deleteBatchByStatusAndThreshold(EventStatus.PROCESSED, now.minus(Duration.ofHours(24)), 100))
                .thenReturn(7);

        // when
        int deleted = tested.deleteProcessedBatch(Duration.ofHours(24), 100);

        // then
        assertThat(deleted).isEqualTo(7);
    }
}
