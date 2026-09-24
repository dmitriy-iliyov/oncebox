package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * PostgreSQL-specific implementation of {@link OutboxDlqRepository}.
 */
public class PostgreSqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    private final Clock clock;

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlUuidHelper uuidHelper, ResultSetMapper mapper, Clock clock) {
        super(jdbcTemplate, uuidHelper, mapper);
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String sql = """
            WITH to_lock AS (
                SELECT id FROM outbox_dlq_events
                WHERE dlq_status = ?
                ORDER BY moved_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_dlq_events
                SET dlq_status = ?, updated_at = ?
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setString(3, lockStatus.name());
                    ps.setTimestamp(4, Timestamp.from(clock.instant()));
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }

    @Override
    public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id FROM outbox_dlq_events 
                WHERE dlq_status = ? AND updated_at <= ?
                LIMIT ?
            )
            DELETE FROM outbox_dlq_events 
            WHERE id IN (SELECT id FROM to_delete)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                }
        );
    }
}
