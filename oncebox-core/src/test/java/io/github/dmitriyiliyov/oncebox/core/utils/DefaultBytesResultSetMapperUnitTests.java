package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBytesResultSetMapperUnitTests {

    @Mock
    private ResultSet rs;

    private DefaultBytesResultSetMapper tested;

    @BeforeEach
    void setUp() {
        tested = new DefaultBytesResultSetMapper();
    }

    @Test
    @DisplayName("UT fromBytesToUuid() should convert 16 bytes array to UUID correctly")
    void fromBytesToUuid_shouldConvertBytesToUuidCorrectly() {
        // given
        UUID expectedUuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(expectedUuid.getMostSignificantBits());
        buffer.putLong(expectedUuid.getLeastSignificantBits());
        byte[] bytes = buffer.array();

        // when
        UUID result = tested.fromBytesToUuid(bytes);

        // then
        assertThat(result).isEqualTo(expectedUuid);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() should throw IllegalArgumentException when bytes length is not 16")
    void fromBytesToUuid_shouldThrowException_whenBytesLengthIsNot16() {
        // given
        byte[] bytes = new byte[15];

        // when & then
        assertThatThrownBy(() -> tested.fromBytesToUuid(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UUID byte array must be 16 bytes long");
    }

    @Test
    @DisplayName("UT toEvent() should map ResultSet to OutboxEvent correctly using fromBytesToUuid")
    void toEvent_shouldMapResultSetToOutboxEventCorrectly() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        byte[] idBytes = buffer.array();

        EventStatus status = EventStatus.PENDING;
        String eventType = "test-event";
        String payloadType = "test-payload";
        String payload = "{}";
        int retryCount = 0;
        Instant now = Instant.now();
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);

        when(rs.getBytes("id")).thenReturn(idBytes);
        when(rs.getString("status")).thenReturn(status.name());
        when(rs.getString("event_type")).thenReturn(eventType);
        when(rs.getString("payload_type")).thenReturn(payloadType);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getInt("retry_count")).thenReturn(retryCount);
        when(rs.getObject("next_retry_at", OffsetDateTime.class)).thenReturn(timestamp);
        when(rs.getObject("created_at", OffsetDateTime.class)).thenReturn(timestamp);
        when(rs.getObject("updated_at", OffsetDateTime.class)).thenReturn(timestamp);

        // when
        OutboxEvent result = tested.toEvent(rs);

        // then
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getEventType()).isEqualTo(eventType);
        assertThat(result.getPayloadType()).isEqualTo(payloadType);
        assertThat(result.getPayload()).isEqualTo(payload);
        assertThat(result.getRetryCount()).isEqualTo(retryCount);
        assertThat(result.getNextRetryAt()).isEqualTo(now);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UT toDlqEvent() should map ResultSet to OutboxDlqEvent correctly using fromBytesToUuid")
    void toDlqEvent_shouldMapResultSetToOutboxDlqEventCorrectly() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        byte[] idBytes = buffer.array();

        EventStatus status = EventStatus.FAILED;
        String eventType = "test-event";
        String payloadType = "test-payload";
        String payload = "{}";
        int retryCount = 3;
        Instant now = Instant.now();
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        DlqStatus dlqStatus = DlqStatus.MOVED;

        when(rs.getBytes("id")).thenReturn(idBytes);
        when(rs.getString("status")).thenReturn(status.name());
        when(rs.getString("event_type")).thenReturn(eventType);
        when(rs.getString("payload_type")).thenReturn(payloadType);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getInt("retry_count")).thenReturn(retryCount);
        when(rs.getObject("next_retry_at", OffsetDateTime.class)).thenReturn(timestamp);
        when(rs.getObject("created_at", OffsetDateTime.class)).thenReturn(timestamp);
        when(rs.getObject("updated_at", OffsetDateTime.class)).thenReturn(timestamp);
        when(rs.getString("dlq_status")).thenReturn(dlqStatus.name());
        when(rs.getObject("moved_at", OffsetDateTime.class)).thenReturn(timestamp);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(rs);

        // then
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getEventType()).isEqualTo(eventType);
        assertThat(result.getPayloadType()).isEqualTo(payloadType);
        assertThat(result.getPayload()).isEqualTo(payload);
        assertThat(result.getRetryCount()).isEqualTo(retryCount);
        assertThat(result.getNextRetryAt()).isEqualTo(now);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getDlqStatus()).isEqualTo(dlqStatus);
        assertThat(result.getMovedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes valid should return UUID")
    void fromBytesToUuid_whenBytesValid_shouldReturnUuid() {
        // given
        UUID expected = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(expected.getMostSignificantBits());
        bb.putLong(expected.getLeastSignificantBits());
        byte[] bytes = bb.array();

        // when
        UUID result = tested.fromBytesToUuid(bytes);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes length less than 16 should throw IAE")
    void fromBytesToUuid_whenBytesLengthLessThan16_shouldThrow() {
        // given
        byte[] bytes = new byte[15];

        // when + then
        assertThatThrownBy(() -> tested.fromBytesToUuid(bytes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes length more than 16 should throw IAE")
    void fromBytesToUuid_whenBytesLengthMoreThan16_shouldThrow() {
        // given
        byte[] bytes = new byte[17];

        // when + then
        assertThatThrownBy(() -> tested.fromBytesToUuid(bytes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes is null should throw NPE")
    void fromBytesToUuid_whenBytesNull_shouldThrow() {
        // when + then
        assertThatThrownBy(() -> tested.fromBytesToUuid(null))
                .isInstanceOf(NullPointerException.class);
    }
}
