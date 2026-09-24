package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.publisher.AbstractOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * PostgreSQL-specific implementation of {@link OutboxRepository}.
 * <ul>
 *
 *     <li>uses the PostgreSQL-specific clause
 *     <code>FOR UPDATE SKIP LOCKED</code> (available since PostgreSQL 9.5) to allow multiple service instances
 *     to process outbox batches in parallel without conflicting on the same rows.</li>
 *
 *     <li>performs batched deletion of processed events using a CTE
 *     (<code>WITH ... AS</code>), ensuring predictable load on the database.</li>
 *
 * </ul>
 */
public class PostgreSqlOutboxRepository extends AbstractOutboxRepository {

    protected final ResultSetMapper mapper;

    public PostgreSqlOutboxRepository(JdbcTemplate jdbcTemplate,
                                      Clock clock,
                                      SqlUuidHelper uuidHelper,
                                      ResultSetMapper mapper) {
        super(jdbcTemplate, clock, uuidHelper);
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType,
                                                                  EventStatus status,
                                                                  int batchSize,
                                                                  UUID lockToken,
                                                                  EventStatus lockStatus) {
        String sql = """
            WITH to_lock AS (
                SELECT id FROM outbox_events
                WHERE event_type = ? AND status = ? AND next_retry_at <= ?
                ORDER BY next_retry_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?, updated_at = ?, lock_token = ?
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setTimestamp(3, Timestamp.from(clock.instant()));
                    ps.setInt(4, batchSize);
                    ps.setString(5, lockStatus.name());
                    ps.setTimestamp(6, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 7, lockToken);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
    }

    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String sql = """
            WITH to_lock AS (
                SELECT id FROM outbox_events
                WHERE status = ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setString(3, lockStatus.name());
                    ps.setTimestamp(4, Timestamp.from(clock.instant()));
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
    }

    @Override
    public int updateBatchStatusByStatusAndThreshold(EventStatus status,
                                                     Instant threshold,
                                                     int batchSize,
                                                     EventStatus newStatus) {
        String sql = """
            WITH to_update AS (
                SELECT id FROM outbox_events
                WHERE status = ? AND updated_at <= ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN (SELECT id FROM to_update)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                    ps.setString(4, newStatus.name());
                    ps.setTimestamp(5, Timestamp.from(clock.instant()));
                }
        );
    }

    @Override
    public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id FROM outbox_events 
                WHERE status = ? AND updated_at <= ?
                ORDER BY updated_at
                LIMIT ?
            )
            DELETE FROM outbox_events 
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
