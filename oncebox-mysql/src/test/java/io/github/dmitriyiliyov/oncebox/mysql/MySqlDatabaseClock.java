package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

class MySqlDatabaseClock implements TestDatabaseClock {

    private final JdbcTemplate jdbcTemplate;
    private final MySqlUuidHelper uuidHelper = new MySqlUuidHelper();

    MySqlDatabaseClock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void shift(String table, String column, UUID id, Duration delta) {
        jdbcTemplate.update(
                "UPDATE %s SET %s = %s + INTERVAL ? MICROSECOND WHERE id = ?".formatted(table, column, column),
                delta.toNanos() / 1000, uuidHelper.uuidToBytes(id)
        );
    }

    @Override
    public Duration distanceFromNow(String table, String column, UUID id) {
        Long micros = jdbcTemplate.queryForObject(
                "SELECT TIMESTAMPDIFF(MICROSECOND, UTC_TIMESTAMP(6), %s) FROM %s WHERE id = ?".formatted(column, table),
                Long.class, uuidHelper.uuidToBytes(id)
        );
        return Duration.ofNanos(micros * 1000);
    }

    @Override
    public Object toColumnValue(Instant moment) {
        return LocalDateTime.ofInstant(moment, ZoneOffset.UTC);
    }
}
