package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RabbitEventClassMapperUnitTests {

    private static final String HEADER_NAME = OnceboxHeaders.EVENT_TYPE.getValue();
    private RabbitEventClassMapper classMapper;

    private static class DummyEvent {}

    @BeforeEach
    void setUp() {
        Map<String, Class<?>> testMappings = Map.of(
                "test-event", DummyEvent.class,
                "string-event", String.class
        );
        classMapper = new RabbitEventClassMapper(testMappings);
    }

    @Test
    @DisplayName("UT fromClass() should always throw IllegalStateException")
    void fromClass_shouldAlwaysThrowIllegalStateException() {
        MessageProperties properties = new MessageProperties();

        assertThatThrownBy(() -> classMapper.fromClass(DummyEvent.class, properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("this method is not implemented and should not be called");
    }

    @Test
    @DisplayName("UT toClass() should return mapped class when header and mapping exist")
    void toClass_shouldReturnMappedClass_whenHeaderAndMappingExist() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(HEADER_NAME, "test-event");

        Class<?> result = classMapper.toClass(properties);

        assertThat(result).isEqualTo(DummyEvent.class);
    }

    @Test
    @DisplayName("UT toClass() should throw exception when header is missing")
    void toClass_shouldThrowException_whenHeaderIsMissing() {
        MessageProperties properties = new MessageProperties();

        assertThatThrownBy(() -> classMapper.toClass(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Header '%s' is missing in message".formatted(HEADER_NAME));
    }

    @Test
    @DisplayName("UT toClass() should throw exception when mapping not found")
    void toClass_shouldThrowException_whenMappingNotFound() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(HEADER_NAME, "unknown-event");

        assertThatThrownBy(() -> classMapper.toClass(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No mapping found for event type: unknown-event");
    }
}
