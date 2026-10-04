package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqApiServiceMetricsDecorator implements OutboxDlqApiService {

    private final Map<ActionType, Counter> actionCounters;
    private final OutboxDlqApiService delegate;

    public OutboxDlqApiServiceMetricsDecorator(MeterRegistry registry, OutboxDlqApiService delegate) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.actionCounters = Arrays.stream(ActionType.values())
                .collect(Collectors.toMap(
                                Function.identity(),
                                type -> registry.counter(
                                        "outbox_dlq_events_by_action_type_rate_total",
                                        "action_type", type.toString().toLowerCase())
                        )
                );
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public OutboxDlqEvent findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<OutboxDlqEvent> findBatch(BatchRequest request) {
        return delegate.findBatch(request);
    }

    @Override
    public long count(DlqStatus status, String eventType) {
        return delegate.count(status, eventType);
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        delegate.updateStatus(id, status);
    }

    @Override
    public BatchModificationResponse updateBatchStatus(BatchUpdateRequest request) {
        return delegate.updateBatchStatus(request);
    }

    @Override
    public int deleteById(UUID id) {
        int deletedCount = delegate.deleteById(id);
        actionCounters.get(ActionType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public BatchModificationResponse deleteBatch(BatchDeleteRequest request) {
        BatchModificationResponse response = delegate.deleteBatch(request);
        actionCounters.get(ActionType.MANUAL_DELETED).increment(response.processedCount());
        return response;
    }
}
