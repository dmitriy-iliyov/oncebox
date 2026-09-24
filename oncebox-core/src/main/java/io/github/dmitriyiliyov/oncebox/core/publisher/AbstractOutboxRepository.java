package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;

/**
 *  Abstract multi SQL dialect implementation of {@link OutboxRepository}.
 *  Provides a mechanism for incrementing retry counters and marking permanently failed events.
 */
public abstract class AbstractOutboxRepository implements OutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final Clock clock;
    protected final SqlUuidHelper uuidHelper;

    public AbstractOutboxRepository(JdbcTemplate jdbcTemplate, Clock clock, SqlUuidHelper uuidHelper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
    }

    @Override
    public void save(OutboxEvent event) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    uuidHelper.setToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getEventType());
                    ps.setString(4, event.getPayloadType());
                    ps.setString(5, event.getPayload());
                    ps.setInt(6, event.getRetryCount());
                    ps.setTimestamp(7, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(8, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getUpdatedAt()));
                }
        );
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    uuidHelper.setToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getEventType());
                    ps.setString(4, event.getPayloadType());
                    ps.setString(5, event.getPayload());
                    ps.setInt(6, event.getRetryCount());
                    ps.setTimestamp(7, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(8, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getUpdatedAt()));
                }
        );
    }

    @Override
    public int updateBatchStatusByLockToken(Set<UUID> ids, UUID lockToken, EventStatus newStatus) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        if (EventStatus.FAILED.equals(newStatus)) {
            throw new IllegalArgumentException("Use partiallyUpdateBatchByLockToken() for update FAILED batch");
        }
        String sql = """
                UPDATE outbox_events 
                SET status = ?, updated_at = ? 
                WHERE lock_token = ? AND id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 3, lockToken);
                    uuidHelper.setToPs(ps, 4, ids);
                }
        );
    }

    @Override
    public int partiallyUpdateBatchByLockToken(List<OutboxEvent> events, UUID lockToken) {
        if (events == null || events.isEmpty()) return 0;
        String sql = """
            UPDATE outbox_events
            SET
                retry_count = ?,
                status = ?,
                next_retry_at = ?,
                updated_at = ?
            WHERE id = ? AND lock_token = ?
        """;
        int [][] result = jdbcTemplate.batchUpdate(
                sql,
                events,
                events.size(),
                (ps, event) -> {
                    ps.setInt(1, event.getRetryCount());
                    ps.setString(2, event.getStatus().name());
                    ps.setTimestamp(3, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(4, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 5, event.getId());
                    uuidHelper.setToPs(ps, 6, lockToken);
                }
        );
        return Arrays.stream(result)
                .flatMapToInt(Arrays::stream)
                .sum();
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        String sql = "DELETE FROM outbox_events WHERE id IN (%s)".formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}