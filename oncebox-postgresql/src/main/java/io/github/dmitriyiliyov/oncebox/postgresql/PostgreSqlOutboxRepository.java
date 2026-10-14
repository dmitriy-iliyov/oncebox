package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.outbox.AbstractOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PostgreSqlOutboxRepository extends AbstractOutboxRepository {

    protected final ResultSetMapper mapper;

    public PostgreSqlOutboxRepository(JdbcTemplate jdbcTemplate,
                                      SqlUuidHelper uuidHelper,
                                      ResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper);
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }


    @Override
    protected void setCreatedAt(PreparedStatement ps, int parameterIndex, Instant createdAt) throws SQLException {
        ps.setObject(parameterIndex, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
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
                WHERE event_type = ? AND status = ? AND next_retry_at <= clock_timestamp()
                ORDER BY next_retry_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?, updated_at = clock_timestamp(), lock_token = ?
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING *
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setInt(3, batchSize);
                    ps.setString(4, lockStatus.name());
                    uuidHelper.setToPs(ps, 5, lockToken);
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
                SET status = ?, updated_at = clock_timestamp()
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
                (rs, rowNum) -> mapper.toEvent(rs)
        );
    }

    @Override
    protected String updateBatchStatusByLockTokenSql(Set<UUID> ids) {
        return """
             UPDATE outbox_events 
             SET status = ?, updated_at = clock_timestamp()
             WHERE lock_token = ? AND id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
    }

    @Override
    protected String partiallyUpdateBatchByLockTokenSql() {
        return """
            UPDATE outbox_events
            SET
                retry_count = ?,
                status = ?,
                next_retry_at = COALESCE(clock_timestamp() + (? * INTERVAL '1 millisecond'), next_retry_at),
                updated_at = clock_timestamp()
            WHERE id = ? AND lock_token = ?
        """;
    }

    @Override
    public int updateBatchStatusByStatusOlderThan(EventStatus status,
                                                  Duration age,
                                                  int batchSize,
                                                  EventStatus newStatus) {
        String sql = """
            WITH to_update AS (
                SELECT id FROM outbox_events
                WHERE status = ? AND updated_at <= clock_timestamp() - (? * INTERVAL '1 millisecond')
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?, updated_at = clock_timestamp()
            WHERE id IN (SELECT id FROM to_update)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setLong(2, age.toMillis());
                    ps.setInt(3, batchSize);
                    ps.setString(4, newStatus.name());
                }
        );
    }

    @Override
    public int deleteBatchByStatusOlderThan(EventStatus status, Duration age, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id FROM outbox_events 
                WHERE status = ? AND updated_at <= clock_timestamp() - (? * INTERVAL '1 millisecond')
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
                    ps.setLong(2, age.toMillis());
                    ps.setInt(3, batchSize);
                }
        );
    }
}
