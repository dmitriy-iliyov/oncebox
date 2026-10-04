package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDlqApiServiceMetricsDecoratorUnitTests {

    @Mock
    private OutboxDlqApiService delegate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter mockCounter;

    private OutboxDlqApiServiceMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(mockCounter);
        tested = new OutboxDlqApiServiceMetricsDecorator(meterRegistry, delegate);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqApiServiceMetricsDecorator(null, delegate));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when delegate is null")
    void constructor_shouldThrowNPE_whenDelegateIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqApiServiceMetricsDecorator(meterRegistry, null));
    }

    @Test
    @DisplayName("UT findById() should strictly delegate call")
    void findById_shouldDelegate() {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent expectedEvent = mock(OutboxDlqEvent.class);
        when(delegate.findById(id)).thenReturn(expectedEvent);

        OutboxDlqEvent actualEvent = tested.findById(id);

        assertEquals(expectedEvent, actualEvent);
        verify(delegate).findById(id);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT findBatch() should strictly delegate call")
    void findBatch_shouldDelegate() {
        BatchRequest request = mock(BatchRequest.class);
        List<OutboxDlqEvent> expectedList = List.of(mock(OutboxDlqEvent.class));
        when(delegate.findBatch(request)).thenReturn(expectedList);

        List<OutboxDlqEvent> actualList = tested.findBatch(request);

        assertEquals(expectedList, actualList);
        verify(delegate).findBatch(request);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT count() should strictly delegate call")
    void count_shouldDelegate() {
        DlqStatus status = DlqStatus.MOVED;
        String eventType = "TEST_EVENT";
        when(delegate.count(status, eventType)).thenReturn(42L);

        long actualCount = tested.count(status, eventType);

        assertEquals(42L, actualCount);
        verify(delegate).count(status, eventType);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT updateStatus() should strictly delegate call")
    void updateStatus_shouldDelegate() {
        UUID id = UUID.randomUUID();
        DlqStatus status = DlqStatus.RESOLVED;

        tested.updateStatus(id, status);

        verify(delegate).updateStatus(id, status);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT updateBatchStatus() should strictly delegate call")
    void updateBatchStatus_shouldDelegate() {
        BatchUpdateRequest request = mock(BatchUpdateRequest.class);
        BatchModificationResponse expectedResponse = mock(BatchModificationResponse.class);
        when(delegate.updateBatchStatus(request)).thenReturn(expectedResponse);

        BatchModificationResponse actualResponse = tested.updateBatchStatus(request);

        assertEquals(expectedResponse, actualResponse);
        verify(delegate).updateBatchStatus(request);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT deleteById() should delegate and increment manual deleted counter")
    void deleteById_shouldDelegateAndIncrementCounter() {
        UUID id = UUID.randomUUID();
        int expectedDeletedCount = 1;
        when(delegate.deleteById(id)).thenReturn(expectedDeletedCount);

        int actualDeletedCount = tested.deleteById(id);

        assertEquals(expectedDeletedCount, actualDeletedCount);
        verify(delegate).deleteById(id);
        verify(mockCounter).increment((double) expectedDeletedCount);
    }

    @Test
    @DisplayName("UT deleteBatch() should delegate and increment manual deleted counter by deleted amount")
    void deleteBatch_shouldDelegateAndIncrementCounter() {
        BatchDeleteRequest request = mock(BatchDeleteRequest.class);
        BatchModificationResponse mockResponse = mock(BatchModificationResponse.class);
        int expectedDeletedCount = 2;

        when(mockResponse.processedCount()).thenReturn(expectedDeletedCount);
        when(delegate.deleteBatch(request)).thenReturn(mockResponse);

        BatchModificationResponse actualResponse = tested.deleteBatch(request);

        assertEquals(mockResponse, actualResponse);
        verify(delegate).deleteBatch(request);
        verify(mockCounter).increment((double) expectedDeletedCount);
    }

    @Test
    @DisplayName("UT deleteBatch() when nothing deleted should increment counter by 0")
    void deleteBatch_whenNothingDeleted_shouldIncrementCounterByZero() {
        BatchDeleteRequest request = mock(BatchDeleteRequest.class);
        BatchModificationResponse mockResponse = mock(BatchModificationResponse.class);
        long expectedDeletedCount = 0;

        when(delegate.deleteBatch(request)).thenReturn(mockResponse);

        BatchModificationResponse actualResponse = tested.deleteBatch(request);

        assertEquals(mockResponse, actualResponse);
        verify(delegate).deleteBatch(request);
        verify(mockCounter).increment(0.0);
    }
}
