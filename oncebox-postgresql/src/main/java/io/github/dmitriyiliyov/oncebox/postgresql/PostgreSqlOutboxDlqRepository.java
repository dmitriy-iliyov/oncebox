package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

public class PostgreSqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlUuidHelper uuidHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper, mapper);
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
                SET dlq_status = ?, updated_at = clock_timestamp()
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING *
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setString(3, lockStatus.name());
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }

    @Override
    public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id FROM outbox_dlq_events 
                WHERE dlq_status = ? AND updated_at <= clock_timestamp() - (? * INTERVAL '1 millisecond')
                LIMIT ?
            )
            DELETE FROM outbox_dlq_events 
            WHERE id IN (SELECT id FROM to_delete)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setLong(2, age.toMillis());
                    ps.setInt(3, batchSize);
                }
        );
    }
}
