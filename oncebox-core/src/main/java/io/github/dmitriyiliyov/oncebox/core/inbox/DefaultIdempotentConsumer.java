package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultIdempotentConsumer implements IdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdempotentConsumer.class);
    private static final int LOGGED_IDS_LIMIT = 10;

    private final TransactionTemplate transactionTemplate;
    private final InboxManager inboxManager;

    public DefaultIdempotentConsumer(TransactionTemplate transactionTemplate,
                                     InboxManager inboxManager) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.inboxManager = Objects.requireNonNull(inboxManager, "inboxManager cannot be null");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (inboxManager.tryConsume(eventId)) {
                    operation.run();
                } else {
                    log.debug("Event {} already consumed, skipped", eventId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
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
            transactionTemplate.executeWithoutResult(status -> {
                if (inboxManager.tryConsume(eventId)) {
                    operation.accept(message);
                } else {
                    log.debug("Event {} already consumed, skipped", eventId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
        }
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        if (ids == null || ids.isEmpty()) {
            log.warn("Provided ids set is null or empty");
            return;
        }
        Objects.requireNonNull(operation, "operation cannot be null");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> alreadyConsumedIds = inboxManager.tryConsumeAndGetDuplicates(ids);
                Set<UUID> validIds = new HashSet<>(ids);
                alreadyConsumedIds.forEach(validIds::remove);
                logSkipped(alreadyConsumedIds.size(), ids.size());
                if (validIds.isEmpty()) {
                    return;
                }
                operation.accept(validIds);
            });
        } catch(Exception e) {
            log.error(
                    "Failed to check batch idempotency and execute operation for {} ids: {}",
                    ids.size(), preview(ids), e
            );
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, EventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        if (messages == null || messages.isEmpty()) {
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
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> alreadyConsumedIds = inboxManager.tryConsumeAndGetDuplicates(messageMap.keySet());
                alreadyConsumedIds.forEach(messageMap::remove);
                logSkipped(alreadyConsumedIds.size(), messages.size());
                if (messageMap.isEmpty()) {
                    return;
                }
                operation.accept(new ArrayList<>(messageMap.values()));
            });
        } catch(Exception e) {
            log.error(
                    "Failed to check batch idempotency and execute operation for {} messages: {}",
                    messageMap.size(), preview(messageMap.keySet()), e
            );
            throw e;
        }
    }

    private static void logSkipped(int skippedCount, int totalCount) {
        if (skippedCount > 0) {
            log.debug("Skipped {} already consumed events of {}", skippedCount, totalCount);
        }
    }

    private static String preview(Collection<UUID> ids) {
        if (ids.size() <= LOGGED_IDS_LIMIT) {
            return ids.toString();
        }
        return ids.stream()
                .limit(LOGGED_IDS_LIMIT)
                .map(UUID::toString)
                .collect(Collectors.joining(", ", "[", ", ... and %d more]".formatted(ids.size() - LOGGED_IDS_LIMIT)));
    }
}
