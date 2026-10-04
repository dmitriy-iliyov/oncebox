package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;

import java.util.Map;

public class RabbitEventClassMapper implements ClassMapper {

    private static final String HEADER_VALUE = OnceboxHeaders.EVENT_TYPE.getValue();
    private final Map<String, Class<?>> mappings;

    public RabbitEventClassMapper(Map<String, Class<?>> mappings) {
        this.mappings = mappings;
    }

    @Override
    public void fromClass(Class<?> aClass, MessageProperties messageProperties) {
        throw new IllegalStateException("this method is not implemented and should not be called");
    }

    @Override
    public Class<?> toClass(MessageProperties messageProperties) {
        Object headerValue = messageProperties.getHeaders().get(HEADER_VALUE);

        if (headerValue == null) {
            throw new IllegalArgumentException("Header '%s' is missing in message".formatted(HEADER_VALUE));
        }

        String eventType = headerValue.toString();
        Class<?> mappedClass = mappings.get(eventType);

        if (mappedClass == null) {
            throw new IllegalArgumentException("No mapping found for event type: " + eventType);
        }

        return mappedClass;
    }
}
