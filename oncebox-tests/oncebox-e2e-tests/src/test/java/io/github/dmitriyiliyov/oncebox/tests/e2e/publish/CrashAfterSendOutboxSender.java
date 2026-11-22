package io.github.dmitriyiliyov.oncebox.tests.e2e.publish;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps the library's sender and, while crashing is on, throws right after the broker has acknowledged a batch, so
 * the polling cycle ends before the batch is marked. The database is left as a killed publisher leaves it: the batch
 * committed as {@code IN_PROCESS} by its capture, with nothing after it.
 * <p>
 * The crash is an {@link Error}: the processor answers an {@link Exception} from the sender by marking the whole
 * batch failed and due for a retry, which is a failed send, not a dead publisher.
 */
public class CrashAfterSendOutboxSender implements OutboxSender {

    private final OutboxSender delegate;
    private final AtomicBoolean crashing = new AtomicBoolean();
    private final AtomicInteger acknowledgedSends = new AtomicInteger();

    public CrashAfterSendOutboxSender(OutboxSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
        SenderResult result = delegate.sendEvents(topic, events);
        acknowledgedSends.addAndGet(result.processedIds().size());
        if (crashing.get()) {
            throw new SimulatedPublisherCrash(result.processedIds().size(), events.size());
        }
        return result;
    }

    public void crashAfterEverySend() {
        crashing.set(true);
    }

    public void stopCrashing() {
        crashing.set(false);
    }

    /**
     * Returns how many events the broker has acknowledged since the last {@link #reset()}, a resent event counted
     * once per send.
     */
    public int acknowledgedSends() {
        return acknowledgedSends.get();
    }

    public void reset() {
        crashing.set(false);
        acknowledgedSends.set(0);
    }

    public static class SimulatedPublisherCrash extends Error {

        SimulatedPublisherCrash(int acknowledged, int batchSize) {
            super("Simulated publisher crash after the broker acknowledged %d of %d events"
                    .formatted(acknowledged, batchSize));
        }
    }
}
