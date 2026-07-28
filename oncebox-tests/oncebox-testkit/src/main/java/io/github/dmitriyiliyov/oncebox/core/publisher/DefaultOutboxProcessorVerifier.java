package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DefaultOutboxProcessorVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final OutboxRepository outboxRepository;
    private final DefaultOutboxProcessor processor;
    private final OutboxSender outboxSenderMock;
    private final IdExtractor idExtractor;
    private final IdPreparer idPreparer;
    private final ResultSetMapper mapper;

    @FunctionalInterface
    public interface IdExtractor {
        UUID extract(Object raw);
    }

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }

    public DefaultOutboxProcessorVerifier(
            JdbcTemplate jdbcTemplate,
            OutboxRepository outboxRepository,
            DefaultOutboxProcessor processor,
            OutboxSender outboxSenderMock,
            IdExtractor idExtractor, IdPreparer idPreparer, ResultSetMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxRepository = outboxRepository;
        this.processor = processor;
        this.outboxSenderMock = outboxSenderMock;
        this.idExtractor = idExtractor;
        this.idPreparer = idPreparer;
        this.mapper = mapper;
    }

    public void process_nullProperties_throwsNullPointerException() {
        assertThatThrownBy(() -> processor.process(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties cannot be null");
    }

    public void process_emptyOutbox_doesNothing() {
        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties(10, 3, 2, 10L);
        processor.process(properties);
        verify(outboxSenderMock, never()).sendEvents(any(), any());
    }

    public void process_allEventsSentSuccessfully_statusChangedToProcessed() {
        OutboxEvent event1 = saveOutboxEvent(EventStatus.PENDING, 0);
        OutboxEvent event2 = saveOutboxEvent(EventStatus.PENDING, 0);
        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties(10, 3, 2, 10L);

        when(outboxSenderMock.sendEvents(eq(properties.getTopic()), any())).thenAnswer(invocation -> {
            List<OutboxEvent> events = invocation.getArgument(1);
            Set<UUID> allIds = events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
            return new SenderResult(allIds, Collections.emptySet());
        });

        processor.process(properties);

        List<UUID> processedIds = queryIdsByStatus("PROCESSED");
        assertThat(processedIds).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(queryIdsByStatus("PENDING")).isEmpty();
        verify(outboxSenderMock, times(1)).sendEvents(eq(properties.getTopic()), any());
    }

    public void process_someEventsFailed_retryCountIncrementedAndNextRetryUpdated() {
        OutboxEvent successEvent = saveOutboxEvent(EventStatus.PENDING, 1);
        OutboxEvent failEvent = saveOutboxEvent(EventStatus.PENDING, 1);
        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties(10, 3, 2, 10L);

        when(outboxSenderMock.sendEvents(eq(properties.getTopic()), any())).thenReturn(
                new SenderResult(
                        new HashSet<>(Set.of(successEvent.getId())),
                        new HashSet<>(Set.of(failEvent.getId()))
                )
        );

        processor.process(properties);

        assertThat(queryIdsByStatus("PROCESSED")).containsOnly(successEvent.getId());

        OutboxEvent event = getEvent(failEvent.getId());
        assertThat(event.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(2);
        long expectedDelaySeconds = (long) Math.pow(properties.backoffMultiplier(), failEvent.getRetryCount() + 1) * properties.backoffDelay();
        Instant expectedTime = Instant.now().plusSeconds(expectedDelaySeconds);

        assertThat(event.getNextRetryAt()).isCloseTo(expectedTime, within(2, ChronoUnit.SECONDS));
    }

    public void process_senderThrowsException_allEventsTreatedAsFailed() {
        OutboxEvent event1 = saveOutboxEvent(EventStatus.PENDING, 0);
        OutboxEvent event2 = saveOutboxEvent(EventStatus.PENDING, 0);
        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties(10, 3, 2, 10L);

        when(outboxSenderMock.sendEvents(eq(properties.getTopic()), any()))
                .thenThrow(new RuntimeException("Kafka is down"));

        processor.process(properties);

        List<UUID> pendingIds = queryIdsByStatus("PENDING");
        assertThat(pendingIds).containsExactlyInAnyOrder(event1.getId(), event2.getId());

        io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent e = getEvent(event1.getId());
        assertThat(e.getRetryCount()).isEqualTo(1);
    }

    public void process_maxRetriesExceeded_statusChangedToFailed() {
        int maxRetries = 3;
        OutboxEvent event = saveOutboxEvent(EventStatus.PENDING, maxRetries);
        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties(10, maxRetries, 2, 10L);

        when(outboxSenderMock.sendEvents(eq(properties.getTopic()), any())).thenReturn(
                new SenderResult(Collections.emptySet(), Set.of(event.getId()))
        );

        processor.process(properties);

        List<UUID> failedIds = queryIdsByStatus("FAILED");
        assertThat(failedIds).containsOnly(event.getId());
    }

    private List<UUID> queryIdsByStatus(String status) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM outbox_events WHERE status = ?", status
        ).stream().map(row -> idExtractor.extract(row.get("id"))).toList();
    }

    private OutboxEvent getEvent(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM outbox_events WHERE id = ?",
                ps -> ps.setObject(1, idPreparer.prepare(id)),
                (rs, n) -> mapper.toEvent(rs)).getFirst();
    }

    private OutboxEvent saveOutboxEvent(EventStatus status, int retryCount) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant nextRetryTime = now.minusSeconds(60);

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), status, "ORDER_CREATED",
                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
                retryCount, nextRetryTime, now, now
        );
        outboxRepository.saveBatch(List.of(event));
        return event;
    }

    private OutboxPublisherPropertiesHolder.EventPropertiesHolder createProperties(int batchSize, int maxRetries, double backoffMultiplier, long backoffDelay) {
        OutboxPublisherPropertiesHolder.EventPropertiesHolder props = mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class);
        when(props.getEventType()).thenReturn("ORDER_CREATED");
        when(props.getBatchSize()).thenReturn(batchSize);
        when(props.getTopic()).thenReturn("orders.topic");
        when(props.getMaxRetries()).thenReturn(maxRetries);
        when(props.backoffMultiplier()).thenReturn(backoffMultiplier);
        when(props.backoffDelay()).thenReturn(backoffDelay);
        return props;
    }
}