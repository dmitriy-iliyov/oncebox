package io.github.dmitriyiliyov.oncebox.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JacksonOutboxSerializerUnitTests {
    @Mock
    ObjectMapper mapper;

    @Mock
    UuidGenerator uuidGenerator;

    @Mock
    Clock clock;

    JacksonOutboxSerializer serializer;

    @BeforeEach
    void setup() {
        serializer = new JacksonOutboxSerializer(mapper, uuidGenerator, clock);
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new JacksonOutboxSerializer(null, uuidGenerator, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mapper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidGenerator is null should throw NullPointerException")
    void constructor_whenUuidGeneratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new JacksonOutboxSerializer(mapper, null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("uuidGenerator cannot be null");
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new JacksonOutboxSerializer(mapper, uuidGenerator, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("serialize() should return OutboxEvent when serialization succeeds")
    void serialize_whenSuccess_shouldReturnOutboxEvent() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        TestEvent event = new TestEvent("test");
        when(uuidGenerator.generate()).thenReturn(id);
        when(mapper.writeValueAsString(event)).thenReturn("{\"value\":\"test\"}");

        // when
        OutboxEvent result = serializer.serialize("type", event);

        // then
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEventType()).isEqualTo("type");
        assertThat(result.getPayload()).isEqualTo("{\"value\":\"test\"}");
        assertThat(result.getPayloadType()).isEqualTo(TestEvent.class.getName());
        verify(uuidGenerator, times(1)).generate();
        verify(mapper, times(1)).writeValueAsString(event);
    }

    @Test
    @DisplayName("serialize() should wrap JsonProcessingException into OutboxSerializationException")
    void serialize_whenJsonProcessingException_shouldWrapToRuntime() throws Exception {
        // given
        TestEvent event = new TestEvent("fail");
        when(uuidGenerator.generate()).thenReturn(UUID.randomUUID());
        when(mapper.writeValueAsString(event)).thenThrow(JsonProcessingException.class);

        // when + then
        assertThrows(OutboxSerializationException.class, () -> serializer.serialize("type", event));
    }

    @Test
    @DisplayName("serialize() should rethrow unexpected exceptions directly")
    void serialize_whenUnexpectedException_shouldRethrow() {
        // given
        TestEvent event = new TestEvent("error");
        when(uuidGenerator.generate()).thenThrow(new IllegalStateException("UUID error"));

        // when + then
        assertThrows(IllegalStateException.class, () -> serializer.serialize("type", event));
    }

    @Test
    @DisplayName("serialize(List) should return list of OutboxEvents when serialization succeeds")
    void serializeList_whenSuccess_shouldReturnOutboxEventList() throws Exception {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        java.time.Instant now = java.time.Instant.now();
        TestEvent event1 = new TestEvent("test1");
        TestEvent event2 = new TestEvent("test2");

        when(uuidGenerator.generate()).thenReturn(id1, id2);
        when(mapper.writeValueAsString(event1)).thenReturn("{\"value\":\"test1\"}");
        when(mapper.writeValueAsString(event2)).thenReturn("{\"value\":\"test2\"}");
        when(clock.instant()).thenReturn(now);

        // when
        java.util.List<OutboxEvent> result = serializer.serialize("type", java.util.List.of(event1, event2));

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(id1);
        assertThat(result.get(0).getEventType()).isEqualTo("type");
        assertThat(result.get(0).getPayloadType()).isEqualTo(TestEvent.class.getName());
        assertThat(result.get(0).getPayload()).isEqualTo("{\"value\":\"test1\"}");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(now);

        assertThat(result.get(1).getId()).isEqualTo(id2);
        assertThat(result.get(1).getEventType()).isEqualTo("type");
        assertThat(result.get(1).getPayloadType()).isEqualTo(TestEvent.class.getName());
        assertThat(result.get(1).getPayload()).isEqualTo("{\"value\":\"test2\"}");
        assertThat(result.get(1).getCreatedAt()).isEqualTo(now);

        verify(uuidGenerator, times(2)).generate();
        verify(mapper, times(1)).writeValueAsString(event1);
        verify(mapper, times(1)).writeValueAsString(event2);
        verify(clock, times(2)).instant();
    }

    @Test
    @DisplayName("serialize(List) should return empty list when input list is empty")
    void serializeList_whenEmptyList_shouldReturnEmptyList() {
        // given
        java.util.List<TestEvent> events = java.util.List.of();

        // when
        java.util.List<OutboxEvent> result = serializer.serialize("type", events);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(uuidGenerator, mapper, clock);
    }

    record TestEvent(String value) {}
}
