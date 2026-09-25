package io.github.dmitriyiliyov.oncebox.core.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The publishing path of the core module as a whole - publisher, serializer, manager and processor are the real
 * ones; only the neighbouring modules are stood in for: the store (a dialect module), the sender (a transport
 * module) and the configuration (the starter).
 */
class OutboxPublishingComponentTests {

    private static final String EVENT_TYPE = "order-created";
    private static final String TOPIC = "orders";
    private static final Instant START = Instant.parse("2026-09-24T10:00:00Z");
    private static final UUID FIRST_CAPTURE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_CAPTURE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final TestClock clock = new TestClock(START);
    private final InMemoryOutboxRepository repository = new InMemoryOutboxRepository(clock);
    private final RecordingOutboxSender sender = new RecordingOutboxSender();
    private final OutboxPublisherPropertiesHolder.EventPropertiesHolder eventProperties =
            mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class);

    private OutboxPublisher publisher;
    private OutboxManager manager;
    private OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        OutboxPublisherPropertiesHolder properties = mock(OutboxPublisherPropertiesHolder.class);
        when(properties.existEventType(EVENT_TYPE)).thenReturn(true);
        when(eventProperties.getEventType()).thenReturn(EVENT_TYPE);
        when(eventProperties.getTopic()).thenReturn(TOPIC);
        when(eventProperties.getBatchSize()).thenReturn(10);
        when(eventProperties.getMaxRetries()).thenReturn(3);
        when(eventProperties.backoffMultiplier()).thenReturn(2.0);
        when(eventProperties.backoffDelay()).thenReturn(10_000L);

        manager = new DefaultOutboxManager(repository, clock);
        publisher = new DefaultOutboxPublisher(
                properties,
                new JacksonOutboxSerializer(new ObjectMapper(), new UuidV7Generator(), clock),
                manager
        );
        processor = new DefaultOutboxProcessor(manager, sender, clock);
    }

    @Test
    @DisplayName("CT publish() then process() when the broker accepts should send the payload and mark the event PROCESSED")
    void process_whenBrokerAccepts_shouldSendPayloadAndMarkProcessed() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        sender.answer(SenderResult::new, RecordingOutboxSender.ALL_PROCESSED);

        // when
        int processed = processor.process(eventProperties);

        // then
        assertThat(processed).isEqualTo(1);
        assertThat(sender.sent()).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(event.getPayload()).contains("order-1");
        });
        assertThat(sender.topics()).containsExactly(TOPIC);
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("CT process() when the broker is down should retry the event after the base delay and not before")
    void process_whenBrokerIsDown_shouldRetryOnlyAfterNextRetryAt() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        sender.failWith(new IllegalStateException("broker down"));

        // when
        processor.process(eventProperties);

        // then
        OutboxEvent failed = repository.onlyEvent();
        assertThat(failed.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(failed.getRetryCount()).isZero();
        assertThat(failed.getNextRetryAt()).isEqualTo(START.plusSeconds(10));

        assertThat(processor.process(eventProperties)).isZero();

        clock.set(failed.getNextRetryAt());
        sender.answer(SenderResult::new, RecordingOutboxSender.ALL_PROCESSED);
        assertThat(processor.process(eventProperties)).isEqualTo(1);
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("CT process() when every attempt fails should mark the event FAILED after the first send and maxRetries retries")
    void process_whenEveryAttemptFails_shouldMarkFailedAfterMaxRetries() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        sender.failWith(new IllegalStateException("broker down"));

        // when
        for (int attempt = 0; attempt < 4; attempt++) {
            processor.process(eventProperties);
            clock.set(repository.onlyEvent().getNextRetryAt());
        }

        // then
        OutboxEvent event = repository.onlyEvent();
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(sender.sent()).hasSize(4);
        assertThat(processor.process(eventProperties)).isZero();
    }

    @Test
    @DisplayName("CT process() when the broker accepts part of a batch should finalize each event by its own outcome")
    void process_whenBrokerAcceptsPartOfBatch_shouldFinalizeEachEventByItsOutcome() {
        // given
        publisher.publish(EVENT_TYPE, List.of(new OrderCreated("accepted"), new OrderCreated("rejected")));
        sender.answer(
                SenderResult::new,
                events -> events.stream().filter(event -> event.getPayload().contains("accepted"))
        );

        // when
        processor.process(eventProperties);

        // then
        assertThat(repository.events())
                .extracting(event -> event.getPayload().contains("accepted") ? "accepted" : "rejected",
                        OutboxEvent::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("accepted", EventStatus.PROCESSED),
                        tuple("rejected", EventStatus.PENDING)
                );
    }

    @Test
    @DisplayName("CT recoverStuckBatch() when an event stayed IN_PROCESS past the limit should return it to PENDING")
    void recoverStuckBatch_whenEventStuckPastLimit_shouldReturnItToPending() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        manager.loadBatch(EVENT_TYPE, 10, FIRST_CAPTURE);
        clock.advance(Duration.ofMinutes(6));

        // when
        int recovered = manager.recoverStuckBatch(Duration.ofMinutes(5), 100);

        // then
        assertThat(recovered).isEqualTo(1);
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("CT recoverStuckBatch() when an event is IN_PROCESS within the limit should leave it to its poller")
    void recoverStuckBatch_whenEventWithinLimit_shouldLeaveIt() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        manager.loadBatch(EVENT_TYPE, 10, FIRST_CAPTURE);
        clock.advance(Duration.ofMinutes(4));

        // when
        int recovered = manager.recoverStuckBatch(Duration.ofMinutes(5), 100);

        // then
        assertThat(recovered).isZero();
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("CT finalizeBatch() when a stale capture finalizes after the event was captured again should keep the newer outcome")
    void finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        List<OutboxEvent> stale = manager.loadBatch(EVENT_TYPE, 10, FIRST_CAPTURE);
        clock.advance(Duration.ofMinutes(6));
        manager.recoverStuckBatch(Duration.ofMinutes(5), 100);
        List<OutboxEvent> fresh = manager.loadBatch(EVENT_TYPE, 10, SECOND_CAPTURE);
        manager.finalizeBatch(fresh, ids(fresh), null, 0, retry -> clock.instant(), SECOND_CAPTURE);

        // when
        manager.finalizeBatch(stale, null, ids(stale), 0, retry -> clock.instant(), FIRST_CAPTURE);

        // then
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("CT finalizeBatch() when the capture was recovered but not taken again should apply its outcome")
    void finalizeBatch_whenCaptureRecoveredButNotTakenAgain_shouldApplyItsOutcome() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        List<OutboxEvent> stale = manager.loadBatch(EVENT_TYPE, 10, FIRST_CAPTURE);
        clock.advance(Duration.ofMinutes(6));
        manager.recoverStuckBatch(Duration.ofMinutes(5), 100);

        // when
        manager.finalizeBatch(stale, ids(stale), null, 0, retry -> clock.instant(), FIRST_CAPTURE);

        // then
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("CT publish() when the event type is not configured should store nothing")
    void publish_whenEventTypeNotConfigured_shouldStoreNothing() {
        // when / then
        assertThatThrownBy(() -> publisher.publish("unknown", new OrderCreated("order-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.events()).isEmpty();
    }

    @Test
    @DisplayName("CT deleteProcessedBatch() should remove only PROCESSED events older than the ttl")
    void deleteProcessedBatch_shouldRemoveOnlyExpiredProcessedEvents() {
        // given
        publisher.publish(EVENT_TYPE, new OrderCreated("order-1"));
        sender.answer(SenderResult::new, RecordingOutboxSender.ALL_PROCESSED);
        processor.process(eventProperties);
        publisher.publish(EVENT_TYPE, new OrderCreated("order-2"));
        clock.advance(Duration.ofHours(25));

        // when
        int deleted = manager.deleteProcessedBatch(Duration.ofHours(24), 100);

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.onlyEvent().getStatus()).isEqualTo(EventStatus.PENDING);
    }

    record OrderCreated(String orderId) {}

    private static Set<UUID> ids(List<OutboxEvent> events) {
        return events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
    }

    static final class TestClock extends Clock {

        private Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        void set(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /**
     * Keeps the rows the way the dialect repositories do: a lock takes due PENDING rows oldest retry first and
     * stamps {@code updated_at}, thresholds are strict.
     */
    static final class InMemoryOutboxRepository implements OutboxRepository {

        private final Clock clock;
        private final Map<UUID, OutboxEvent> rows = new LinkedHashMap<>();
        private final Map<UUID, UUID> lockTokens = new HashMap<>();

        InMemoryOutboxRepository(Clock clock) {
            this.clock = clock;
        }

        List<OutboxEvent> events() {
            return List.copyOf(rows.values());
        }

        OutboxEvent onlyEvent() {
            assertThat(rows).hasSize(1);
            return rows.values().iterator().next();
        }

        @Override
        public void save(OutboxEvent event) {
            rows.put(event.getId(), event);
        }

        @Override
        public void saveBatch(List<OutboxEvent> eventBatch) {
            if (eventBatch != null) {
                eventBatch.forEach(this::save);
            }
        }

        @Override
        public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType,
                                                                      EventStatus status,
                                                                      int batchSize,
                                                                      UUID lockToken,
                                                                      EventStatus lockStatus) {
            List<OutboxEvent> locked = lock(rows.values().stream()
                    .filter(event -> event.getEventType().equals(eventType))
                    .filter(event -> event.getStatus() == status)
                    .filter(event -> !event.getNextRetryAt().isAfter(clock.instant()))
                    .sorted(Comparator.comparing(OutboxEvent::getNextRetryAt))
                    .limit(batchSize)
                    .toList(), lockStatus);
            locked.forEach(event -> lockTokens.put(event.getId(), lockToken));
            return locked;
        }

        @Override
        public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
            return lock(rows.values().stream()
                    .filter(event -> event.getStatus() == status)
                    .limit(batchSize)
                    .toList(), lockStatus);
        }

        @Override
        public int updateBatchStatusByLockToken(Set<UUID> ids, UUID lockToken, EventStatus newStatus) {
            if (newStatus == EventStatus.FAILED) {
                throw new IllegalArgumentException("use partiallyUpdateBatchByLockToken for failed events");
            }
            if (ids == null) {
                return 0;
            }
            int updated = 0;
            for (UUID id : ids) {
                OutboxEvent event = rows.get(id);
                if (event != null && isHeldBy(id, lockToken)) {
                    rows.put(id, withStatus(event, newStatus));
                    updated++;
                }
            }
            return updated;
        }

        @Override
        public int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize,
                                                         EventStatus newStatus) {
            List<OutboxEvent> matching = rows.values().stream()
                    .filter(event -> event.getStatus() == status)
                    .filter(event -> event.getUpdatedAt().isBefore(threshold))
                    .limit(batchSize)
                    .toList();
            matching.forEach(event -> rows.put(event.getId(), withStatus(event, newStatus)));
            return matching.size();
        }

        @Override
        public int partiallyUpdateBatchByLockToken(List<OutboxEvent> events, UUID lockToken) {
            if (events == null) {
                return 0;
            }
            List<OutboxEvent> held = events.stream()
                    .filter(event -> rows.containsKey(event.getId()) && isHeldBy(event.getId(), lockToken))
                    .toList();
            held.forEach(event -> rows.put(event.getId(), event));
            return held.size();
        }

        @Override
        public int deleteBatch(Set<UUID> ids) {
            if (ids == null) {
                return 0;
            }
            ids.forEach(lockTokens::remove);
            return (int) ids.stream().filter(id -> rows.remove(id) != null).count();
        }

        @Override
        public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
            Set<UUID> expired = rows.values().stream()
                    .filter(event -> event.getStatus() == status)
                    .filter(event -> event.getUpdatedAt().isBefore(threshold))
                    .limit(batchSize)
                    .map(OutboxEvent::getId)
                    .collect(Collectors.toSet());
            return deleteBatch(expired);
        }

        private boolean isHeldBy(UUID id, UUID lockToken) {
            return lockToken != null && lockToken.equals(lockTokens.get(id));
        }

        private List<OutboxEvent> lock(List<OutboxEvent> events, EventStatus lockStatus) {
            List<OutboxEvent> locked = events.stream().map(event -> withStatus(event, lockStatus)).toList();
            locked.forEach(event -> rows.put(event.getId(), event));
            return locked;
        }

        private OutboxEvent withStatus(OutboxEvent event, EventStatus status) {
            return new OutboxEvent(
                    event.getId(),
                    status,
                    event.getEventType(),
                    event.getPayloadType(),
                    event.getPayload(),
                    event.getRetryCount(),
                    event.getNextRetryAt(),
                    event.getCreatedAt(),
                    clock.instant()
            );
        }
    }

    static final class RecordingOutboxSender implements OutboxSender {

        static final Function<List<OutboxEvent>, Stream<OutboxEvent>> ALL_PROCESSED = List::stream;

        private final List<OutboxEvent> sent = new ArrayList<>();
        private final List<String> topics = new ArrayList<>();
        private Function<List<OutboxEvent>, SenderResult> behaviour;

        void answer(BiFunction<Set<UUID>, Set<UUID>, SenderResult> result,
                    Function<List<OutboxEvent>, Stream<OutboxEvent>> accepted) {
            behaviour = events -> {
                Set<UUID> processed = accepted.apply(events).map(OutboxEvent::getId).collect(Collectors.toSet());
                Set<UUID> failed = events.stream()
                        .map(OutboxEvent::getId)
                        .filter(id -> !processed.contains(id))
                        .collect(Collectors.toSet());
                return result.apply(processed, failed);
            };
        }

        void failWith(RuntimeException exception) {
            behaviour = events -> {
                throw exception;
            };
        }

        List<OutboxEvent> sent() {
            return sent;
        }

        List<String> topics() {
            return topics;
        }

        @Override
        public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
            sent.addAll(events);
            topics.add(topic);
            return behaviour.apply(events);
        }
    }
}
