package io.github.dmitriyiliyov.oncebox.core;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class for extracting oncebox headers from Spring {@link Message}.
 */
public final class OnceboxHeadersUtils {

    private OnceboxHeadersUtils() { }

    /**
     * Extract event ID from message headers.
     *
     * @param message Spring message
     * @return event UUID
     * @throws IllegalArgumentException if header is missing or invalid
     */
    public static UUID extractId(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        String headerName = OnceboxHeaders.EVENT_ID.getValue();

        Object objEventId = headers.get(headerName);

        if (objEventId instanceof UUID uuidEventId) {
            return uuidEventId;
        }

        if (objEventId instanceof String strEventId) {
            return eventIdFromString(strEventId, headerName);
        }

        if (objEventId instanceof byte[] bytesEventId) {
            String strEventId = new String(bytesEventId, StandardCharsets.UTF_8);
            return eventIdFromString(strEventId, headerName);
        }

        throw new IllegalArgumentException("Header '%s' not found or has unsupported type".formatted(headerName));
    }

    private static UUID eventIdFromString(String str, String headerName) {
        if (str.isBlank()) {
            throw new IllegalArgumentException("Header '%s' is blank".formatted(headerName));
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Header '%s' has invalid UUID format: %s".formatted(headerName, str), e);
        }
    }

    /**
     * Extract event type from message headers.
     *
     * @param message Spring message
     * @return event type string
     * @throws IllegalArgumentException if header is missing
     */
    public static String extractEventType(Message<?> message) {
        return extractStringHeader(message, OnceboxHeaders.EVENT_TYPE.getValue());
    }

    /**
     * Extract payload type from message headers.
     *
     * @param message Spring message
     * @return event payload type (fully qualified class name)
     * @throws IllegalArgumentException if header is missing
     */
    public static String extractEventPayloadType(Message<?> message) {
        return extractStringHeader(message, OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue());
    }

    private static String extractStringHeader(Message<?> message, String headerName) {
        MessageHeaders headers = message.getHeaders();
        String value = headers.get(headerName, String.class);

        if (value == null) {
            throw new IllegalArgumentException("Header '%s' not found".formatted(headerName));
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException("Header '%s' is blank".formatted(headerName));
        }

        return value;
    }
}