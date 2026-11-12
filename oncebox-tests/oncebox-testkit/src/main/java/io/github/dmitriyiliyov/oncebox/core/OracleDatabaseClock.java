package io.github.dmitriyiliyov.oncebox.core;

import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * The Oracle database clock: ids are sixteen bytes, times are compared in UTC against {@code SYSTIMESTAMP}.
 */
public class OracleDatabaseClock implements TestDatabaseClock {

    private final JdbcTemplate jdbcTemplate;

    public OracleDatabaseClock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public void shift(String table, String column, UUID id, Duration delta) {
        jdbcTemplate.update(
                "UPDATE %s SET %s = %s + NUMTODSINTERVAL(? / 1000000, 'SECOND') WHERE id = ?"
                        .formatted(table, column, column),
                delta.toNanos() / 1000, toBytes(id)
        );
    }

    @Override
    public Duration distanceFromNow(String table, String column, UUID id) {
        // both sides are taken to UTC first, as a column written with another offset than SYSTIMESTAMP keeps it
        Long millis = jdbcTemplate.queryForObject(
                """
                SELECT ROUND(EXTRACT(DAY FROM d) * 86400000 + EXTRACT(HOUR FROM d) * 3600000
                       + EXTRACT(MINUTE FROM d) * 60000 + EXTRACT(SECOND FROM d) * 1000)
                FROM (SELECT SYS_EXTRACT_UTC(%s) - SYS_EXTRACT_UTC(SYSTIMESTAMP) AS d FROM %s WHERE id = ?)
                """.formatted(column, table),
                Long.class, toBytes(id)
        );
        return Duration.ofMillis(millis);
    }

    // the layout of OracleSqlUuidHelper, inlined so the testkit does not depend on the dialect module
    private static byte [] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
