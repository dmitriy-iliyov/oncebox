package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class OracleTestOutboxRepository extends AbstractTestOutboxRepository {

    public OracleTestOutboxRepository(JdbcTemplate jdbcTemplate) {
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
    protected String minusSeconds(String column) {
        return column + " - NUMTODSINTERVAL(?, 'SECOND')";
    }

    @Override
    protected List<String> truncateStatements() {
        return List.of(
                "DELETE FROM outbox_events",
                "DELETE FROM outbox_dlq_events",
                "DELETE FROM inbox_events",
                "DELETE FROM e2e_produced_events",
                "DELETE FROM e2e_consumed_events"
        );
    }
}
