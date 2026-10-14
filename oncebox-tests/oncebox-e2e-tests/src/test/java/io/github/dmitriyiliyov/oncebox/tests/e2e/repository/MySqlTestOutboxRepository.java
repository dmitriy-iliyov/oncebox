package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class MySqlTestOutboxRepository extends AbstractTestOutboxRepository {

    public MySqlTestOutboxRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected Object idParam(UUID id) {
        return UuidBytes.toBytes(id);
    }

    @Override
    protected Object verifyIdParam(UUID id) {
        return id.toString();
    }

    @Override
    protected UUID readId(ResultSet rs, String column) throws SQLException {
        return UuidBytes.fromBytes(rs.getBytes(column));
    }

    @Override
    protected String now() {
        return "UTC_TIMESTAMP(6)";
    }

    @Override
    protected String minusSeconds(String column) {
        return "DATE_SUB(" + column + ", INTERVAL ? SECOND)";
    }

    @Override
    protected List<String> truncateStatements() {
        return List.of(
                "TRUNCATE TABLE outbox_events",
                "TRUNCATE TABLE outbox_dlq_events",
                "TRUNCATE TABLE inbox_events",
                "TRUNCATE TABLE e2e_produced_events",
                "TRUNCATE TABLE e2e_consumed_events"
        );
    }
}
