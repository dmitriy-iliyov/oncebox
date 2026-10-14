package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.UUID;

class OracleDatabaseClock implements TestDatabaseClock {

    private final JdbcTemplate jdbcTemplate;
    private final OracleSqlUuidHelper uuidHelper = new OracleSqlUuidHelper();

    OracleDatabaseClock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void shift(String table, String column, UUID id, Duration delta) {
        jdbcTemplate.update(
                "UPDATE %s SET %s = %s + NUMTODSINTERVAL(? / 1000000, 'SECOND') WHERE id = ?"
                        .formatted(table, column, column),
                delta.toNanos() / 1000, uuidHelper.uuidToBytes(id)
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
                Long.class, uuidHelper.uuidToBytes(id)
        );
        return Duration.ofMillis(millis);
    }
}
