package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultOutboxDlqTransfer implements OutboxDlqTransfer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqTransfer.class);

    private final TransactionTemplate transactionTemplate;
    private final OutboxManager manager;
    private final OutboxDlqManager dlqManager;
    private final OutboxDlqEventMapper eventMapper;
    private final OutboxDlqHandler handler;

    public DefaultOutboxDlqTransfer(TransactionTemplate transactionTemplate,
                                    OutboxManager manager,
                                    OutboxDlqManager dlqManager,
                                    OutboxDlqEventMapper eventMapper,
                                    OutboxDlqHandler handler) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.dlqManager = Objects.requireNonNull(dlqManager, "dlqManager cannot be null");
        this.eventMapper = Objects.requireNonNull(eventMapper, "eventMapper cannot be null");
        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
    }

    @Override
    public int transferToDlq(int batchSize) {
        final List<OutboxEvent> events = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            try {
                events.addAll(manager.loadBatch(EventStatus.FAILED, batchSize));
                if (events.isEmpty()) {
                    return;
                }
                dlqManager.saveBatch(eventMapper.toDlqEvents(events));
                manager.deleteBatch(
                        events.stream()
                                .map(OutboxEvent::getId)
                                .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring events from outbox to DLQ", e);
                throw e;
            }
        });
        if (!events.isEmpty()) {
            try {
                handler.handle(events);
            } catch (Exception e) {
                log.error("Error when handle events after transferring to DLQ", e);
            }
            return events.size();
        }
        return 0;
    }

    @Override
    public int transferFromDlq(int batchSize) {
        Integer transferredCount = transactionTemplate.execute(status -> {
            try {
                List<OutboxDlqEvent> dlqEvents = dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
                if (dlqEvents == null || dlqEvents.isEmpty()) {
                    return 0;
                }
                manager.saveBatch(eventMapper.toOutboxEvents(dlqEvents));
                dlqManager.deleteBatch(
                        dlqEvents.stream()
                                .map(OutboxDlqEvent::getId)
                                .collect(Collectors.toSet())
                );
                return dlqEvents.size();
            } catch (Exception e) {
                log.error("Error when transferring events from DLQ to outbox", e);
                throw e;
            }
        });
        return transferredCount != null ? transferredCount : 0;
    }
}
