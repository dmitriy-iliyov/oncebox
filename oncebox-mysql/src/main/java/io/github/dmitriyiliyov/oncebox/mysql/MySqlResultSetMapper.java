package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Reads the {@code DATETIME} columns of MySQL, which hold UTC wall time, as moments - independent of the time
 * zone of the JVM and of the connection.
 */
public class MySqlResultSetMapper extends DefaultBytesResultSetMapper {

    @Override
    protected Instant getInstant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
    }
}
