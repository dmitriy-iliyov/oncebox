package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DefaultOutboxDlqApiService implements OutboxDlqApiService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqApiService.class);

    private final OutboxDlqApiRepository repository;

    public DefaultOutboxDlqApiService(OutboxDlqApiRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Transactional(readOnly = true)
    @Override
    public OutboxDlqEvent findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> findBatch(BatchRequest request) {
        DlqFilter filter = DlqFilter.builder()
                .status(request.status())
                .eventType(request.eventType())
                .build();
        return repository.findBatch(filter, request.batchNumber(), request.batchSize());
    }

    @Override
    public long count(DlqStatus status, String eventType) {
        DlqFilter filter = DlqFilter.builder()
                .status(status)
                .eventType(eventType)
                .build();
        return repository.count(filter);
    }

    @Transactional
    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        OutboxDlqEvent event = repository.findByIdForUpdate(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
        if (DlqStatus.IN_PROCESS.equals(event.getDlqStatus())) {
            log.debug("Update of DLQ event {} rejected, it is in 'IN_PROCESS' status", id);
            throw new OutboxDlqEventInProcessException(event.getId());
        }
        repository.updateStatus(id, status);
    }

    @Transactional
    @Override
    public BatchModificationResponse updateBatchStatus(BatchUpdateRequest request) {
        DlqFilter filter = DlqFilter.builder()
                .status(request.status())
                .eventType(request.eventType())
                .ids(request.ids())
                .build();
        int updatedCount = repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);
        if (request.hasValidIds()) {
            return BatchModificationResponse.ofUpdate(request.ids().size(), updatedCount);
        }
        return BatchModificationResponse.ofUpdate(updatedCount);
    }

    @Transactional
    @Override
    public int deleteById(UUID id) {
        OutboxDlqEvent event = repository.findByIdForUpdate(id).orElseThrow(
                () -> new OutboxDlqEventNotFoundException(id)
        );
        if (DlqStatus.IN_PROCESS.equals(event.getDlqStatus())) {
            log.debug("Delete of DLQ event {} rejected, it is in 'IN_PROCESS' status", id);
            throw new OutboxDlqEventInProcessException(event.getId());
        }
        return repository.deleteById(id);
    }

    @Transactional
    @Override
    public BatchModificationResponse deleteBatch(BatchDeleteRequest request) {
        DlqFilter filter = DlqFilter.builder()
                .eventType(request.eventType())
                .ids(request.ids())
                .build();
        int deletedCount = repository.deleteBatch(filter, DlqStatus.IN_PROCESS);
        if (request.hasValidIds()) {
            return BatchModificationResponse.ofDelete(request.ids().size(), deletedCount);
        }
        return BatchModificationResponse.ofDelete(deletedCount);
    }
}
