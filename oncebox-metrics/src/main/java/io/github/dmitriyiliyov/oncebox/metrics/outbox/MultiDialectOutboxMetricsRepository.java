package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

public class MultiDialectOutboxMetricsRepository implements OutboxMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public MultiDialectOutboxMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countByStatus(EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }
}
