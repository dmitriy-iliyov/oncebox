package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultResultSetMapperUnitTests {

    @Mock
    private ResultSet rs;

    private DefaultResultSetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DefaultResultSetMapper();
    }

    @Test
    @DisplayName("UT toEvent() should map ResultSet to OutboxEvent correctly")
    void toEvent_shouldMapResultSetToOutboxEventCorrectly() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        EventStatus status = EventStatus.PENDING;
        String eventType = "test-event";
        String payloadType = "test-payload";
        String payload = "{}";
        int retryCount = 0;
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);

        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getString("status")).thenReturn(status.name());
        when(rs.getString("event_type")).thenReturn(eventType);
        when(rs.getString("payload_type")).thenReturn(payloadType);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getInt("retry_count")).thenReturn(retryCount);
        when(rs.getTimestamp("next_retry_at")).thenReturn(timestamp);
        when(rs.getTimestamp("created_at")).thenReturn(timestamp);
        when(rs.getTimestamp("updated_at")).thenReturn(timestamp);

        // when
        OutboxEvent result = mapper.toEvent(rs);

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
    @DisplayName("UT toDlqEvent() should map ResultSet to OutboxDlqEvent correctly")
    void toDlqEvent_shouldMapResultSetToOutboxDlqEventCorrectly() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        EventStatus status = EventStatus.FAILED;
        String eventType = "test-event";
        String payloadType = "test-payload";
        String payload = "{}";
        int retryCount = 3;
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        DlqStatus dlqStatus = DlqStatus.MOVED;

        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getString("status")).thenReturn(status.name());
        when(rs.getString("event_type")).thenReturn(eventType);
        when(rs.getString("payload_type")).thenReturn(payloadType);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getInt("retry_count")).thenReturn(retryCount);
        when(rs.getTimestamp("next_retry_at")).thenReturn(timestamp);
        when(rs.getTimestamp("created_at")).thenReturn(timestamp);
        when(rs.getTimestamp("updated_at")).thenReturn(timestamp);
        when(rs.getString("dlq_status")).thenReturn(dlqStatus.name());
        when(rs.getTimestamp("moved_at")).thenReturn(timestamp);

        // when
        OutboxDlqEvent result = mapper.toDlqEvent(rs);

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
}
