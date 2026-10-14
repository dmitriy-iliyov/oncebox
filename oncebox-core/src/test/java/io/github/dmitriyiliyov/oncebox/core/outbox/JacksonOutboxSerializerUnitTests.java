package io.github.dmitriyiliyov.oncebox.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JacksonOutboxSerializerUnitTests {

    private static final UUID ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ID_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");

    @Mock
    private ObjectMapper mapper;

    @Mock
    private UuidGenerator uuidGenerator;

    @InjectMocks
    private JacksonOutboxSerializer tested;

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new JacksonOutboxSerializer(null, uuidGenerator))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("mapper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidGenerator is null should throw NullPointerException")
    void constructor_whenUuidGeneratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new JacksonOutboxSerializer(mapper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("uuidGenerator cannot be null");
    }

    @Test
    @DisplayName("UT serialize() when the event is serialized should return a new pending event without times")
    void serialize_whenEventIsSerialized_shouldReturnNewPendingEventWithoutTimes() throws Exception {
        // given
        TestEvent event = new TestEvent("test");
        when(uuidGenerator.generate()).thenReturn(ID_1);
        when(mapper.writeValueAsString(event)).thenReturn("{\"value\":\"test\"}");

        // when
        OutboxEvent result = tested.serialize("type", event);

        // then
        assertThat(result.getId()).isEqualTo(ID_1);
        assertThat(result.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(result.getEventType()).isEqualTo("type");
        assertThat(result.getPayloadType()).isEqualTo(TestEvent.class.getName());
        assertThat(result.getPayload()).isEqualTo("{\"value\":\"test\"}");
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("UT serialize() when Jackson fails should throw OutboxSerializationException")
    void serialize_whenJacksonFails_shouldThrowOutboxSerializationException() throws Exception {
        // given
        TestEvent event = new TestEvent("fail");
        when(uuidGenerator.generate()).thenReturn(ID_1);
        when(mapper.writeValueAsString(event)).thenThrow(JsonProcessingException.class);

        // when / then
        assertThatThrownBy(() -> tested.serialize("type", event))
                .isInstanceOf(OutboxSerializationException.class)
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("UT serialize() when the id generator fails should let its exception through")
    void serialize_whenUuidGeneratorFails_shouldLetExceptionThrough() {
        // given
        when(uuidGenerator.generate()).thenThrow(new IllegalStateException("UUID error"));

        // when / then
        assertThatThrownBy(() -> tested.serialize("type", new TestEvent("error")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UUID error");
    }

    @Test
    @DisplayName("UT serialize(List) should serialize every event under its own id, in order")
    void serializeList_whenEventsAreSerialized_shouldKeepOrderAndGiveEachItsId() throws Exception {
        // given
        TestEvent event1 = new TestEvent("test1");
        TestEvent event2 = new TestEvent("test2");
        when(uuidGenerator.generate()).thenReturn(ID_1, ID_2);
        when(mapper.writeValueAsString(event1)).thenReturn("{\"value\":\"test1\"}");
        when(mapper.writeValueAsString(event2)).thenReturn("{\"value\":\"test2\"}");

        // when
        List<OutboxEvent> result = tested.serialize("type", List.of(event1, event2));

        // then
        assertThat(result).extracting(OutboxEvent::getId).containsExactly(ID_1, ID_2);
        assertThat(result).extracting(OutboxEvent::getPayload)
                .containsExactly("{\"value\":\"test1\"}", "{\"value\":\"test2\"}");
    }

    @Test
    @DisplayName("UT serialize(List) when the list is empty should return empty list without generating ids")
    void serializeList_whenListIsEmpty_shouldReturnEmptyListWithoutGeneratingIds() {
        // when
        List<OutboxEvent> result = tested.serialize("type", List.of());

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(uuidGenerator, mapper);
    }

    record TestEvent(String value) {}
}
