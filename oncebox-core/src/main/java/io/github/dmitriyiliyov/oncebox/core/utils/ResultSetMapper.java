package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the current row of a {@link ResultSet} over {@code outbox_events} or {@code outbox_dlq_events} to an event,
 * reading the columns by name.
 */
public interface ResultSetMapper {
    OutboxEvent toEvent(ResultSet rs) throws SQLException;
    OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException;
}
