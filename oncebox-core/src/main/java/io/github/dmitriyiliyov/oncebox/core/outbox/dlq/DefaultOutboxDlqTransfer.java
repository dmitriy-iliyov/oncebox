package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.utils.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
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
                if (ListUtils.isEmpty(events)) {
                    return;
                }
                dlqManager.saveBatch(eventMapper.toDlqEvents(events));
                manager.deleteBatch(
                        events.stream()
                                .map(OutboxEvent::getId)
                                .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring outbox events to DLQ, batchSize={}, loaded={}", batchSize, events.size(), e);
                throw e;
            }
        });
        if (!events.isEmpty()) {
            log.info("Transferred {} failed outbox events to DLQ, eventTypes={}", events.size(), eventTypes(events));
            try {
                handler.handle(events);
            } catch (Exception e) {
                log.error("Error in DLQ handler after transferring {} outbox events to DLQ", events.size(), e);
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
                if (ListUtils.isEmpty(dlqEvents)) {
                    return 0;
                }
                manager.saveBatchWithCreatedAt(eventMapper.toOutboxEvents(dlqEvents));
                dlqManager.deleteBatch(
                        dlqEvents.stream()
                                .map(OutboxDlqEvent::getId)
                                .collect(Collectors.toSet())
                );
                return dlqEvents.size();
            } catch (Exception e) {
                log.error("Error when transferring DLQ events back to outbox, batchSize={}", batchSize, e);
                throw e;
            }
        });
        int count = transferredCount != null ? transferredCount : 0;
        if (count > 0) {
            log.info("Transferred {} DLQ events back to outbox for retry", count);
        }
        return count;
    }

    private static Set<String> eventTypes(List<OutboxEvent> events) {
        return events.stream()
                .map(OutboxEvent::getEventType)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
