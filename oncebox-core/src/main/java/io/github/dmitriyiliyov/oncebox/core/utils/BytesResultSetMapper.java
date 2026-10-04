package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public abstract class BytesResultSetMapper implements ResultSetMapper {

    @Override
    public OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                fromBytesToUuid(rs.getBytes("id")),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    @Override
    public OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException {
        return new OutboxDlqEvent(
                fromBytesToUuid(rs.getBytes("id")),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                DlqStatus.fromString(rs.getString("dlq_status")),
                rs.getTimestamp("moved_at").toInstant()
        );
    }

    public abstract UUID fromBytesToUuid(byte [] bytes);
}
