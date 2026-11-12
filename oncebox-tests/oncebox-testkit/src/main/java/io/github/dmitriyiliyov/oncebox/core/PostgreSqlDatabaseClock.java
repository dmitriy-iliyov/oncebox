package io.github.dmitriyiliyov.oncebox.core;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * The PostgreSQL database clock: ids are native {@code UUID}s, times are read against {@code clock_timestamp()}.
 */
public class PostgreSqlDatabaseClock implements TestDatabaseClock {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlDatabaseClock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public void shift(String table, String column, UUID id, Duration delta) {
        jdbcTemplate.update(
                "UPDATE %s SET %s = %s + (? * INTERVAL '1 microsecond') WHERE id = ?".formatted(table, column, column),
                delta.toNanos() / 1000, id
        );
    }

    @Override
    public Duration distanceFromNow(String table, String column, UUID id) {
        Long millis = jdbcTemplate.queryForObject(
                "SELECT (EXTRACT(EPOCH FROM (%s - clock_timestamp())) * 1000)::bigint FROM %s WHERE id = ?"
                        .formatted(column, table),
                Long.class, id
        );
        return Duration.ofMillis(millis);
    }
}
