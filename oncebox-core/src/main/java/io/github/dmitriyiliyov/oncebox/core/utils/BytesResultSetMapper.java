package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
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
                getInstant(rs, "next_retry_at"),
                getInstant(rs, "created_at"),
                getInstant(rs, "updated_at")
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
                getInstant(rs, "next_retry_at"),
                getInstant(rs, "created_at"),
                getInstant(rs, "updated_at"),
                DlqStatus.fromString(rs.getString("dlq_status")),
                getInstant(rs, "moved_at")
        );
    }

    protected Instant getInstant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    public abstract UUID fromBytesToUuid(byte [] bytes);
}
