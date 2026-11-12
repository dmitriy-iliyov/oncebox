package io.github.dmitriyiliyov.oncebox.core;

import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * The MySQL database clock: ids are sixteen bytes, times are kept without a zone and read against
 * {@code UTC_TIMESTAMP(6)}.
 */
public class MySqlDatabaseClock implements TestDatabaseClock {

    private final JdbcTemplate jdbcTemplate;

    public MySqlDatabaseClock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public void shift(String table, String column, UUID id, Duration delta) {
        jdbcTemplate.update(
                "UPDATE %s SET %s = %s + INTERVAL ? MICROSECOND WHERE id = ?".formatted(table, column, column),
                delta.toNanos() / 1000, toBytes(id)
        );
    }

    @Override
    public Duration distanceFromNow(String table, String column, UUID id) {
        Long micros = jdbcTemplate.queryForObject(
                "SELECT TIMESTAMPDIFF(MICROSECOND, UTC_TIMESTAMP(6), %s) FROM %s WHERE id = ?".formatted(column, table),
                Long.class, toBytes(id)
        );
        return Duration.ofNanos(micros * 1000);
    }

    @Override
    public Object toColumnValue(Instant moment) {
        return LocalDateTime.ofInstant(moment, ZoneOffset.UTC);
    }

    // the layout of MySqlUuidHelper, inlined so the testkit does not depend on the dialect module
    private static byte [] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
