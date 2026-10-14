package io.github.dmitriyiliyov.oncebox.core.inbox;

import io.github.dmitriyiliyov.oncebox.core.utils.ListUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractIdempotentConsumer implements IdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(AbstractIdempotentConsumer.class);
    protected final InboxManager inboxManager;

    public AbstractIdempotentConsumer(InboxManager inboxManager) {
        this.inboxManager = Objects.requireNonNull(inboxManager, "inboxManager cannot be null");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        try {
            consumeAndRunOperation(eventId, operation);
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
        }
    }

    protected void consumeAndRunOperation(UUID eventId, Runnable operation) {
        if (inboxManager.tryConsume(eventId)) {
            operation.run();
        } else {
            logAlreadyConsumed(eventId);
        }
    }

    @Override
    public <T> void consume(T message, EventIdExtractor<T> idExtractor, Consumer<T> operation) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");

        UUID eventId = idExtractor.extract(message);
        Objects.requireNonNull(eventId, "eventId cannot be null");

        try {
            consumeAndRunOperation(message, eventId, operation);
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
        }
    }

    protected <T> void consumeAndRunOperation(T message, UUID eventId, Consumer<T> operation) {
        if (inboxManager.tryConsume(eventId)) {
            operation.accept(message);
        } else {
            logAlreadyConsumed(eventId);
        }
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        if (SetUtils.isEmpty(ids)) {
            log.warn("Provided ids set is null or empty");
            return;
        }
        Objects.requireNonNull(operation, "operation cannot be null");

        try {
            consumeAndRunOperation(ids, operation);
        } catch(Exception e) {
            log.error(
                    "Failed to check batch idempotency and execute operation for {} ids: {}",
                    ids.size(), LogUtils.preview(ids), e
            );
            throw e;
        }
    }

    protected void consumeAndRunOperation(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        Set<UUID> alreadyConsumedIds = inboxManager.tryConsumeAndGetDuplicates(ids);
        Set<UUID> validIds = new HashSet<>(ids);
        alreadyConsumedIds.forEach(validIds::remove);
        logSkipped(alreadyConsumedIds.size(), ids.size());
        if (validIds.isEmpty()) {
            return;
        }
        operation.accept(validIds);
    }

    @Override
    public <T> void consume(List<T> messages, EventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        if (ListUtils.isEmpty(messages)) {
            log.warn("Provided messages set is null or empty");
            return;
        }
        Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");

        Map<UUID, T> messageMap = messages.stream()
                .collect(Collectors.toMap(
                        idExtractor::extract,
                        Function.identity(),
                        (existing, recipient) -> existing)
                );
        try {
            consumeAndRunOperation(messages, messageMap, operation);
        } catch(Exception e) {
            log.error(
                    "Failed to check batch idempotency and execute operation for {} messages: {}",
                    messageMap.size(), LogUtils.preview(messageMap.keySet()), e
            );
            throw e;
        }
    }

    protected <T> void consumeAndRunOperation(List<T> messages,
                                              Map<UUID, T> messageMap,
                                              Consumer<List<T>> operation) {
        Set<UUID> alreadyConsumedIds = inboxManager.tryConsumeAndGetDuplicates(messageMap.keySet());
        alreadyConsumedIds.forEach(messageMap::remove);
        logSkipped(alreadyConsumedIds.size(), messages.size());
        if (messageMap.isEmpty()) {
            return;
        }
        operation.accept(new ArrayList<>(messageMap.values()));
    }

    private static void logAlreadyConsumed(UUID eventId) {
        log.debug("Event {} already consumed, skipped", eventId);
    }

    private static void logSkipped(int skippedCount, int totalCount) {
        if (skippedCount > 0) {
            log.debug("Skipped {} already consumed events of {}", skippedCount, totalCount);
        }
    }
}
