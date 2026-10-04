package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DefaultOutboxDlqEventMapper implements OutboxDlqEventMapper {

    private final Clock clock;

    public DefaultOutboxDlqEventMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public OutboxDlqEvent toDlqEvent(OutboxEvent event) {
        if (event == null) {
            return null;
        }
        return new OutboxDlqEvent(
                event.getId(),
                EventStatus.FAILED,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                DlqStatus.MOVED,
                clock.instant()
        );
    }

    @Override
    public List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .map(this::toDlqEvent)
                .toList();
    }

    @Override
    public OutboxEvent toOutboxEvent(OutboxDlqEvent event) {
        if (event == null) {
            return null;
        }
        return new OutboxEvent(
                event.getId(),
                EventStatus.PENDING,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                -1,
                clock.instant(),
                event.getCreatedAt(),
                clock.instant()
        );
    }

    @Override
    public List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .map(this::toOutboxEvent)
                .toList();
    }
}
