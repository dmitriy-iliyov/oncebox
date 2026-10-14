package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.outbox.AbstractOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class MySqlOutboxRepository extends AbstractOutboxRepository {

    protected final ResultSetMapper mapper;

    public MySqlOutboxRepository(JdbcTemplate jdbcTemplate,
                                 SqlUuidHelper uuidHelper,
                                 BytesResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper);
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    protected void setCreatedAt(PreparedStatement ps, int parameterIndex, Instant createdAt) throws SQLException {
        ps.setObject(parameterIndex, LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    }

    @Override
    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType,
                                                                  EventStatus status,
                                                                  int batchSize,
                                                                  UUID lockToken,
                                                                  EventStatus lockStatus) {
        String selectSql = """
            SELECT * 
            FROM outbox_events
            WHERE event_type = ? AND status = ? AND next_retry_at <= UTC_TIMESTAMP(6)
            ORDER BY next_retry_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return lock(events, lockStatus, lockToken);
    }

    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String selectSql = """
            SELECT * 
            FROM outbox_events
            WHERE status = ?
            ORDER BY updated_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return lock(events, lockStatus, null);
    }

    /**
     * Sets the status of the selected events and, when {@code lockToken} is given, the token of this capture;
     * without one the event keeps the token it already carries. {@code updated_at} is set by the database clock,
     * so the returned events keep the {@code updatedAt} they were selected with.
     */
    private List<OutboxEvent> lock(List<OutboxEvent> events, EventStatus lockStatus, UUID lockToken) {
        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());

        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }

        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = UTC_TIMESTAMP(6)%s
            WHERE id IN(%s)
        """.formatted(lockToken == null ? "" : ", lock_token = ?", RepositoryUtils.generateIdsPlaceholders(ids));

        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    int index = 2;
                    if (lockToken != null) {
                        uuidHelper.setToPs(ps, index++, lockToken);
                    }
                    uuidHelper.setToPs(ps, index, ids);
                }
        );

        events.forEach(event -> event.setStatus(lockStatus));
        return events;
    }

    @Override
    protected String updateBatchStatusByLockTokenSql(Set<UUID> ids) {
        return """
            UPDATE outbox_events
            SET status = ?, updated_at = UTC_TIMESTAMP(6)
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
                next_retry_at = COALESCE(UTC_TIMESTAMP(6) + INTERVAL (? * 1000) MICROSECOND, next_retry_at),
                updated_at = UTC_TIMESTAMP(6)
            WHERE id = ? AND lock_token = ?
        """;
    }

    @Override
    public int updateBatchStatusByStatusOlderThan(EventStatus status, Duration age, int batchSize, EventStatus newStatus) {
        String sql = """
            UPDATE outbox_events
                SET status = ?, updated_at = UTC_TIMESTAMP(6)
            WHERE id IN (
                SELECT id FROM(
                    SELECT id FROM outbox_events
                    WHERE status = ? AND updated_at <= UTC_TIMESTAMP(6) - INTERVAL (? * 1000) MICROSECOND
                    ORDER BY updated_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                ) AS to_update
            )
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, status.name());
                    ps.setLong(3, age.toMillis());
                    ps.setInt(4, batchSize);
                }
        );
    }

    @Override
    public int deleteBatchByStatusOlderThan(EventStatus status, Duration age, int batchSize) {
        String sql = """
            DELETE FROM outbox_events
            WHERE status = ? AND updated_at <= UTC_TIMESTAMP(6) - INTERVAL (? * 1000) MICROSECOND
            ORDER BY updated_at
            LIMIT ?
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