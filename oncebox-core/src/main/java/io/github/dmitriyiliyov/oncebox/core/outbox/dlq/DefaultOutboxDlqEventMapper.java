package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DefaultOutboxDlqEventMapper implements OutboxDlqEventMapper {

    @Override
    public OutboxDlqEvent toDlqEvent(OutboxEvent event) {
        if (event == null) {
            return null;
        }
        return OutboxDlqEvent.moved(event);
    }

    @Override
    public List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events) {
        if (ListUtils.isEmpty(events)) {
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
                null,
                event.getCreatedAt(),
                null
        );
    }

    @Override
    public List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events) {
        if (ListUtils.isEmpty(events)) {
            return Collections.emptyList();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .map(this::toOutboxEvent)
                .toList();
    }
}
