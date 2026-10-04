package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqApiServiceUnitTests {

    @Mock
    OutboxDlqApiRepository repository;

    @InjectMocks
    DefaultOutboxDlqApiService tested;

    @Test
    @DisplayName("UT constructor should throw NPE when repository is null")
    void constructor_shouldThrowNPE_whenRepositoryIsNull() {
        assertThrows(NullPointerException.class, () -> new DefaultOutboxDlqApiService(null));
    }

    @Test
    @DisplayName("UT findById() when event found should return event")
    void findById_whenFound_shouldReturnEvent() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        OutboxDlqEvent result = tested.findById(id);

        // then
        assertEquals(event, result);
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT findById() when event not found should throw OutboxDlqEventNotFoundException")
    void findById_whenNotFound_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when + then
        assertThrows(OutboxDlqEventNotFoundException.class, () -> tested.findById(id));
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT findBatch() should construct DlqFilter, call repository and return list")
    void findBatch_shouldCallRepositoryAndReturnList() {
        // given
        BatchRequest request = mock(BatchRequest.class);
        when(request.status()).thenReturn(DlqStatus.RESOLVED);
        when(request.eventType()).thenReturn("ORDER_CREATED");
        when(request.batchNumber()).thenReturn(1);
        when(request.batchSize()).thenReturn(10);

        List<OutboxDlqEvent> expectedEvents = List.of(mock(OutboxDlqEvent.class));
        when(repository.findBatch(any(DlqFilter.class), eq(1), eq(10))).thenReturn(expectedEvents);

        // when
        List<OutboxDlqEvent> result = tested.findBatch(request);

        // then
        assertEquals(expectedEvents, result);
        verify(repository).findBatch(any(DlqFilter.class), eq(1), eq(10));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT count() should construct DlqFilter and call repository.count()")
    void count_shouldCallRepositoryCount() {
        // given
        when(repository.count(any(DlqFilter.class))).thenReturn(15L);

        // when
        long result = tested.count(DlqStatus.MOVED, "ORDER_CREATED");

        // then
        assertEquals(15L, result);
        verify(repository).count(any(DlqFilter.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateStatus() when event is IN_PROCESS should throw OutboxDlqEventInProcessException")
    void updateStatus_whenEventIsInProcess_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.IN_PROCESS);
        when(event.getId()).thenReturn(id);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

        // when + then
        OutboxDlqEventInProcessException e = assertThrows(
                OutboxDlqEventInProcessException.class, () -> tested.updateStatus(id, DlqStatus.RESOLVED)
        );
        assertEquals("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id), e.getDetail());
        verify(repository).findByIdForUpdate(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateStatus() when event is not found should throw OutboxDlqEventNotFoundException")
    void updateStatus_whenEventNotFound_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        // when + then
        assertThrows(OutboxDlqEventNotFoundException.class, () -> tested.updateStatus(id, DlqStatus.RESOLVED));
        verify(repository).findByIdForUpdate(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateStatus() when event is not IN_PROCESS should update")
    void updateStatus_whenEventNotInProcess_shouldUpdate() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

        // when
        tested.updateStatus(id, DlqStatus.RESOLVED);

        // then
        verify(repository).findByIdForUpdate(id);
        verify(repository).updateStatus(id, DlqStatus.RESOLVED);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when request has valid ids should return BatchResponse with IDs count")
    void updateBatchStatus_whenIdsProvided_shouldReturnResponseWithRequestedIdsCount() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        BatchUpdateRequest request = mock(BatchUpdateRequest.class);
        when(request.status()).thenReturn(DlqStatus.RESOLVED);
        when(request.eventType()).thenReturn("ORDER_CREATED");
        when(request.ids()).thenReturn(ids);
        when(request.hasValidIds()).thenReturn(true);

        when(repository.updateBatchStatus(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS))).thenReturn(2);

        // when
        BatchModificationResponse result = tested.updateBatchStatus(request);

        // then
        assertEquals(2, result.requestedCount());
        assertEquals(2, result.processedCount());
        verify(repository).updateBatchStatus(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when request has NO valid ids should return BatchResponse matching updatedCount")
    void updateBatchStatus_whenIdsNotProvided_shouldReturnResponseWithUpdatedCountAsRequested() {
        // given
        BatchUpdateRequest request = mock(BatchUpdateRequest.class);
        when(request.status()).thenReturn(DlqStatus.RESOLVED);
        when(request.eventType()).thenReturn("ORDER_CREATED");
        when(request.ids()).thenReturn(null);
        when(request.hasValidIds()).thenReturn(false);

        when(repository.updateBatchStatus(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS))).thenReturn(4);

        // when
        BatchModificationResponse result = tested.updateBatchStatus(request);

        // then
        assertEquals(0, result.requestedCount());
        assertEquals(4, result.processedCount());
        assertEquals(result.status(), OperationStatus.POSSIBLE_PARTIAL_SUCCESS);
        verify(repository).updateBatchStatus(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event not found should throw OutboxDlqEventNotFoundException")
    void deleteById_whenEventNotFound_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        // when + then
        assertThrows(OutboxDlqEventNotFoundException.class, () -> tested.deleteById(id));
        verify(repository).findByIdForUpdate(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event is IN_PROCESS should throw OutboxDlqEventInProcessException")
    void deleteById_whenEventInProcess_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.IN_PROCESS);
        when(event.getId()).thenReturn(id);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

        // when + then
        assertThrows(OutboxDlqEventInProcessException.class, () -> tested.deleteById(id));
        verify(repository).findByIdForUpdate(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event valid should delete and return count")
    void deleteById_whenEventValid_shouldDelete() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
        when(repository.deleteById(id)).thenReturn(1);

        // when
        int result = tested.deleteById(id);

        // then
        assertEquals(1, result);
        verify(repository).findByIdForUpdate(id);
        verify(repository).deleteById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when request has valid ids should return BatchResponse with IDs count")
    void deleteBatch_whenIdsProvided_shouldReturnResponseWithRequestedIdsCount() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        BatchDeleteRequest request = mock(BatchDeleteRequest.class);
        when(request.eventType()).thenReturn("ORDER_CREATED");
        when(request.ids()).thenReturn(ids);
        when(request.hasValidIds()).thenReturn(true);

        when(repository.deleteBatch(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS))).thenReturn(2);

        // when
        BatchModificationResponse result = tested.deleteBatch(request);

        // then
        assertEquals(3, result.requestedCount());
        assertEquals(2, result.processedCount());
        assertEquals(result.status(), OperationStatus.PARTIAL_SUCCESS);
        verify(repository).deleteBatch(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when request has NO valid ids should return BatchResponse matching deletedCount")
    void deleteBatch_whenIdsNotProvided_shouldReturnResponseWithDeletedCountAsRequested() {
        // given
        BatchDeleteRequest request = mock(BatchDeleteRequest.class);
        when(request.eventType()).thenReturn("ORDER_CREATED");
        when(request.ids()).thenReturn(null);
        when(request.hasValidIds()).thenReturn(false);

        when(repository.deleteBatch(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS))).thenReturn(5);

        // when
        BatchModificationResponse result = tested.deleteBatch(request);

        // then
        assertEquals(0, result.requestedCount());
        assertEquals(5, result.processedCount());
        verify(repository).deleteBatch(any(DlqFilter.class), eq(DlqStatus.IN_PROCESS));
        verifyNoMoreInteractions(repository);
    }
}