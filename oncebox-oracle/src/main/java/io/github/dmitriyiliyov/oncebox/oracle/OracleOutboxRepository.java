package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.publisher.AbstractOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OracleOutboxRepository extends AbstractOutboxRepository {

    protected final BytesResultSetMapper mapper;

    public OracleOutboxRepository(JdbcTemplate jdbcTemplate,
                                  Clock clock,
                                  SqlUuidHelper uuidHelper,
                                  BytesResultSetMapper mapper) {
        super(jdbcTemplate, clock, uuidHelper);
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
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
            WHERE event_type = ? AND status = ? AND next_retry_at <= ?
            ORDER BY next_retry_at
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(selectSql);
                    ps.setMaxRows(batchSize);
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setTimestamp(3, Timestamp.from(clock.instant()));
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
     * without one the event keeps the token it already carries.
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
                SET status = ?, updated_at = ?%s
            WHERE id IN (%s)
        """.formatted(lockToken == null ? "" : ", lock_token = ?", RepositoryUtils.generateIdsPlaceholders(ids));
        Instant updatedAt = clock.instant();
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(updatedAt));
                    int index = 3;
                    if (lockToken != null) {
                        uuidHelper.setToPs(ps, index++, lockToken);
                    }
                    uuidHelper.setToPs(ps, index, ids);
                }
        );

        events.forEach(event -> {
            event.setStatus(lockStatus);
            event.setUpdatedAt(updatedAt);
        });
        return events;
    }

    @Override
    public int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize, EventStatus newStatus) {
        String selectSql = """
            SELECT id
            FROM outbox_events
            WHERE id IN (
                SELECT id
                FROM outbox_events
                WHERE status = ? AND updated_at <= ?
                ORDER BY updated_at
                FETCH FIRST ? ROWS ONLY
            )
            FOR UPDATE SKIP LOCKED
        """;

        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id")))
        );

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 3, ids);
                }
        );
    }

    @Override
    public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id FROM outbox_events
            WHERE status = ? AND updated_at <= ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
        """;

        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
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
