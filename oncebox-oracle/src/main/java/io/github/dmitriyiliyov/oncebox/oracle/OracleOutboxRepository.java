package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.outbox.AbstractOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class OracleOutboxRepository extends AbstractOutboxRepository {

    protected final BytesResultSetMapper mapper;

    public OracleOutboxRepository(JdbcTemplate jdbcTemplate,
                                  SqlUuidHelper uuidHelper,
                                  BytesResultSetMapper mapper) {
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
        String selectSql = """
            SELECT *
            FROM outbox_events
            WHERE event_type = ? AND status = ? AND next_retry_at <= SYSTIMESTAMP
            ORDER BY next_retry_at
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(selectSql);
                    ps.setMaxRows(batchSize);
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    return ps;
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return updateStatus(events, lockStatus, lockToken);
    }

    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_events
            WHERE status = ?
            ORDER BY updated_at
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(selectSql);
                    ps.setMaxRows(batchSize);
                    ps.setString(1, status.name());
                    return ps;
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return updateStatus(events, lockStatus, null);
    }

    /**
     * Sets the status of the selected events and, when {@code lockToken} is given, the token of this capture;
     * without one the event keeps the token it already carries. {@code updated_at} is set by the database clock,
     * so the returned events keep the {@code updatedAt} they were selected with.
     */
    private List<OutboxEvent> updateStatus(List<OutboxEvent> events, EventStatus lockStatus, UUID lockToken) {
        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());

        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }

        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = SYSTIMESTAMP%s
            WHERE id IN (%s)
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
            SET status = ?, updated_at = SYSTIMESTAMP
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
                next_retry_at = COALESCE(SYSTIMESTAMP + NUMTODSINTERVAL(? / 1000, 'SECOND'), next_retry_at),
                updated_at = SYSTIMESTAMP
            WHERE id = ? AND lock_token = ?
        """;
    }

    @Override
    public int updateBatchStatusByStatusOlderThan(EventStatus status, Duration age, int batchSize, EventStatus newStatus) {
        String selectSql = """
            SELECT id
            FROM outbox_events
            WHERE id IN (
                SELECT id
                FROM outbox_events
                WHERE status = ? AND updated_at <= SYSTIMESTAMP - NUMTODSINTERVAL(? / 1000, 'SECOND')
                ORDER BY updated_at
                FETCH FIRST ? ROWS ONLY
            )
            FOR UPDATE SKIP LOCKED
        """;

        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setLong(2, age.toMillis());
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id")))
        );

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = SYSTIMESTAMP
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    uuidHelper.setToPs(ps, 2, ids);
                }
        );
    }

    @Override
    public int deleteBatchByStatusOlderThan(EventStatus status, Duration age, int batchSize) {
        String selectSql = """
            SELECT id FROM outbox_events
            WHERE status = ? AND updated_at <= SYSTIMESTAMP - NUMTODSINTERVAL(? / 1000, 'SECOND')
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
        """;

        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setLong(2, age.toMillis());
                    ps.setInt(3, batchSize);
                },
                (rs, numRow) -> mapper.fromBytesToUuid(rs.getBytes("id"))
        ));

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String sql = """
            DELETE FROM outbox_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}
