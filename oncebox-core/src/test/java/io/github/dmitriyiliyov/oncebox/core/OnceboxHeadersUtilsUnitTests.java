package io.github.dmitriyiliyov.oncebox.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnceboxHeadersUtilsUnitTests {

    @Test
    @DisplayName("UT extractId() should return UUID from message headers when value is UUID")
    void extractId_shouldReturnUuidWhenValueIsUuid() {
        // given
        UUID expectedId = UUID.randomUUID();
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), expectedId)
                .build();

        // when
        UUID result = OnceboxHeadersUtils.extractId(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT extractId() should return UUID from message headers when value is valid String")
    void extractId_shouldReturnUuidWhenValueIsValidString() {
        // given
        UUID expectedId = UUID.randomUUID();
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), expectedId.toString())
                .build();

        // when
        UUID result = OnceboxHeadersUtils.extractId(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT extractId() should return UUID from message headers when value is valid byte array")
    void extractId_shouldReturnUuidWhenValueIsValidByteArray() {
        // given
        UUID expectedId = UUID.randomUUID();
        byte[] bytes = expectedId.toString().getBytes(StandardCharsets.UTF_8);
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), bytes)
                .build();

        // when
        UUID result = OnceboxHeadersUtils.extractId(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header is blank string")
    void extractId_shouldThrowExceptionIfHeaderIsBlankString() {
        // given
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), "   ")
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' is blank".formatted(OnceboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header has invalid UUID format")
    void extractId_shouldThrowExceptionIfHeaderIsInvalidString() {
        // given
        String invalidUuid = "not-a-uuid";
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), invalidUuid)
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' has invalid UUID format: %s".formatted(OnceboxHeaders.EVENT_ID.getValue(), invalidUuid));
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header is blank byte array")
    void extractId_shouldThrowExceptionIfHeaderIsBlankByteArray() {
        // given
        byte[] bytes = "   ".getBytes(StandardCharsets.UTF_8);
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), bytes)
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' is blank".formatted(OnceboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header byte array has invalid UUID format")
    void extractId_shouldThrowExceptionIfHeaderByteArrayIsInvalid() {
        // given
        String invalidUuid = "not-a-uuid";
        byte[] bytes = invalidUuid.getBytes(StandardCharsets.UTF_8);
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), bytes)
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' has invalid UUID format: %s".formatted(OnceboxHeaders.EVENT_ID.getValue(), invalidUuid));
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header is missing")
    void extractId_shouldThrowExceptionIfHeaderMissing() {
        // given
        Message<String> message = MessageBuilder.withPayload("test").build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found or has unsupported type".formatted(OnceboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT extractId() should throw IllegalArgumentException if header has unsupported type")
    void extractId_shouldThrowExceptionIfHeaderHasUnsupportedType() {
        // given
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), 12345)
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractId(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found or has unsupported type".formatted(OnceboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT extractEventType() should return string from message headers")
    void extractEventType_shouldReturnStringFromHeaders() {
        // given
        String expectedType = "UserCreatedEvent";
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), expectedType)
                .build();

        // when
        String result = OnceboxHeadersUtils.extractEventType(message);

        // then
        assertThat(result).isEqualTo(expectedType);
    }

    @Test
    @DisplayName("UT extractEventType() should throw IllegalArgumentException if header missing")
    void extractEventType_shouldThrowExceptionIfHeaderMissing() {
        // given
        Message<String> message = MessageBuilder.withPayload("test").build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractEventType(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found".formatted(OnceboxHeaders.EVENT_TYPE.getValue()));
    }

    @Test
    @DisplayName("UT extractEventType() should throw IllegalArgumentException if header is blank")
    void extractEventType_shouldThrowExceptionIfHeaderIsBlank() {
        // given
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), "   ")
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractEventType(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' is blank".formatted(OnceboxHeaders.EVENT_TYPE.getValue()));
    }

    @Test
    @DisplayName("UT extractEventPayloadType() should return string from message headers")
    void extractEventPayloadType_shouldReturnStringFromHeaders() {
        // given
        String expectedPayloadType = "com.example.UserPayload";
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), expectedPayloadType)
                .build();

        // when
        String result = OnceboxHeadersUtils.extractEventPayloadType(message);

        // then
        assertThat(result).isEqualTo(expectedPayloadType);
    }

    @Test
    @DisplayName("UT extractEventPayloadType() should throw IllegalArgumentException if header missing")
    void extractEventPayloadType_shouldThrowExceptionIfHeaderMissing() {
        // given
        Message<String> message = MessageBuilder.withPayload("test").build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractEventPayloadType(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found".formatted(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue()));
    }

    @Test
    @DisplayName("UT extractEventPayloadType() should throw IllegalArgumentException if header is blank")
    void extractEventPayloadType_shouldThrowExceptionIfHeaderIsBlank() {
        // given
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), "")
                .build();

        // when + then
        assertThatThrownBy(() -> OnceboxHeadersUtils.extractEventPayloadType(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' is blank".formatted(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue()));
    }
}