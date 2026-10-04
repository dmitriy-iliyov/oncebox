package io.github.dmitriyiliyov.oncebox.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JacksonOutboxSerializer implements OutboxSerializer {

    private final ObjectMapper mapper;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    public JacksonOutboxSerializer(ObjectMapper mapper, UuidGenerator uuidGenerator, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator, "uuidGenerator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public <T> OutboxEvent serialize(String eventType, T event) {
        try {
            UUID id = uuidGenerator.generate();
            String payloadType = event.getClass().getName();
            String payload = mapper.writeValueAsString(event);
            return new OutboxEvent(id, eventType, payloadType, payload, clock.instant());
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException("Error when serialize event", e);
        }
    }

    @Override
    public <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents) {
        return rowEvents.stream()
                .map(event -> serialize(eventType, event))
                .toList();
    }
}
