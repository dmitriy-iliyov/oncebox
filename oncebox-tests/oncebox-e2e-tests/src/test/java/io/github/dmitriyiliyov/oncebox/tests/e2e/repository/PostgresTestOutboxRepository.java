package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PostgresTestOutboxRepository extends AbstractTestOutboxRepository {

    public PostgresTestOutboxRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected Object idParam(UUID id) {
        return id;
    }

    @Override
    protected Object verifyIdParam(UUID id) {
        return id;
    }

    @Override
    protected UUID readId(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    @Override
    protected String minusSeconds(String column) {
        return column + " - make_interval(secs => ?)";
    }

    @Override
    protected List<String> truncateStatements() {
        return List.of(
                "TRUNCATE outbox_events, outbox_dlq_events, inbox_events, e2e_produced_events, e2e_consumed_events"
        );
    }
}
