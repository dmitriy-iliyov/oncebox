package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.utils.ListUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.*;

/**
 *  Abstract multi SQL dialect implementation of {@link OutboxRepository}.
 *  Provides a mechanism for incrementing retry counters and marking permanently failed events.
 */
public abstract class AbstractOutboxRepository implements OutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlUuidHelper uuidHelper;

    public AbstractOutboxRepository(JdbcTemplate jdbcTemplate, SqlUuidHelper uuidHelper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
    }

    @Override
    public void save(OutboxEvent event) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count)
            VALUES (?, ?, ?, ?, ?, ?)
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
                }
        );
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count)
            VALUES (?, ?, ?, ?, ?, ?)
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
                }
        );
    }

    @Override
    public void saveBatchWithCreatedAt(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
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
                    setCreatedAt(ps, 7, Objects.requireNonNull(event.getCreatedAt(), "createdAt cannot be null"));
                }
        );
    }

    /**
     * Binds the creation time of an event to its {@code created_at} column as the same moment, whatever the time zone
     * of the JVM: a dialect whose column keeps a time zone binds it with its offset, one whose column keeps none binds
     * it as UTC wall time, the way the database clock writes that column.
     */
    protected abstract void setCreatedAt(PreparedStatement ps, int parameterIndex, Instant createdAt) throws SQLException;

    @Override
    public int updateBatchStatusByLockToken(Set<UUID> ids, UUID lockToken, EventStatus newStatus) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        if (EventStatus.FAILED.equals(newStatus)) {
            throw new IllegalArgumentException("Use partiallyUpdateBatchByLockToken() for update FAILED batch");
        }
        return jdbcTemplate.update(
                updateBatchStatusByLockTokenSql(ids),
                ps -> {
                    ps.setString(1, newStatus.name());
                    uuidHelper.setToPs(ps, 2, lockToken);
                    uuidHelper.setToPs(ps, 3, ids);
                }
        );
    }

    /**
     * The update that sets the new status of the events with the given ids and lock token, and {@code updated_at} to
     * the database time. Its parameters are bound in this order: the new status, the lock token, then the ids.
     *
     * @param ids the ids the {@code IN} placeholders are generated for.
     * @return    the SQL of the update.
     */
    protected abstract String updateBatchStatusByLockTokenSql(Set<UUID> ids);

    @Override
    public int partiallyUpdateBatchByLockToken(List<OutboxEvent> events, UUID lockToken) {
        if (ListUtils.isEmpty(events)) return 0;
        int [][] result = jdbcTemplate.batchUpdate(
                partiallyUpdateBatchByLockTokenSql(),
                events,
                events.size(),
                (ps, event) -> {
                    ps.setInt(1, event.getRetryCount());
                    ps.setString(2, event.getStatus().name());
                    if (event.getRetryDelay() == null) {
                        ps.setNull(3, Types.BIGINT);
                    } else {
                        ps.setLong(3, event.getRetryDelay().toMillis());
                    }
                    uuidHelper.setToPs(ps, 4, event.getId());
                    uuidHelper.setToPs(ps, 5, lockToken);
                }
        );
        return Arrays.stream(result)
                .flatMapToInt(Arrays::stream)
                .sum();
    }

    /**
     * The update of one event with the given id and lock token: it sets the retry count and the status, sets
     * {@code next_retry_at} to the database time plus the retry delay in milliseconds and {@code updated_at} to the
     * database time. A {@code null} delay leaves {@code next_retry_at} as it is. Its parameters are bound in this
     * order: the retry count, the status, the delay, the id, the lock token.
     */
    protected abstract String partiallyUpdateBatchByLockTokenSql();

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        String sql = "DELETE FROM outbox_events WHERE id IN (%s)".formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}