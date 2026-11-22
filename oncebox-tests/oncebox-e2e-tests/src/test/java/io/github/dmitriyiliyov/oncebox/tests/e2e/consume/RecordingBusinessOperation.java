package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The business operation of the consumers, counting what reached the listener and what reached the operation, so
 * that a test sees how many deliveries the idempotent consumer skipped: deliveries minus runs.
 * <p>
 * An event marked with {@link #failFirstRunOf(UUID)} has its first run write the business row and then throw, the
 * way an operation fails halfway; the listener rethrows and the broker delivers the event again.
 */
public class RecordingBusinessOperation {

    private final TestOutboxRepository repository;
    private final Map<UUID, AtomicInteger> deliveries = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> runs = new ConcurrentHashMap<>();
    private final Set<UUID> failingFirstRun = ConcurrentHashMap.newKeySet();

    public RecordingBusinessOperation(TestOutboxRepository repository) {
        this.repository = repository;
    }

    public void recordDelivery(UUID verifyId) {
        deliveries.computeIfAbsent(verifyId, id -> new AtomicInteger()).incrementAndGet();
    }

    public void run(UUID verifyId) {
        runs.computeIfAbsent(verifyId, id -> new AtomicInteger()).incrementAndGet();
        repository.saveConsumedBusiness(verifyId);
        if (failingFirstRun.remove(verifyId)) {
            throw new IllegalStateException("Simulated failure of the first run for verifyId=%s".formatted(verifyId));
        }
    }

    public void failFirstRunOf(UUID verifyId) {
        failingFirstRun.add(verifyId);
    }

    public int deliveries(UUID verifyId) {
        AtomicInteger count = deliveries.get(verifyId);
        return count == null ? 0 : count.get();
    }

    public int deliveries() {
        return deliveries.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int runs(UUID verifyId) {
        AtomicInteger count = runs.get(verifyId);
        return count == null ? 0 : count.get();
    }

    public int runs() {
        return runs.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public void clear() {
        deliveries.clear();
        runs.clear();
        failingFirstRun.clear();
    }
}
