package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.InvalidDlqFilterException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;

public abstract class AbstractOutboxDlqApiRepository implements OutboxDlqApiRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlUuidHelper uuidHelper;
    protected final ResultSetMapper mapper;
    protected final Clock clock;

    public AbstractOutboxDlqApiRepository(JdbcTemplate jdbcTemplate,
                                          SqlUuidHelper uuidHelper,
                                          ResultSetMapper mapper,
                                          Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public Optional<OutboxDlqEvent> findById(UUID id) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id = ?
        """;
        List<OutboxDlqEvent> results = jdbcTemplate.query(
                sql,
                ps -> uuidHelper.setToPs(ps, 1, id),
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }

    @Override
    public Optional<OutboxDlqEvent> findByIdForUpdate(UUID id) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id = ?
            FOR UPDATE
        """;
        List<OutboxDlqEvent> results = jdbcTemplate.query(
                sql,
                ps -> uuidHelper.setToPs(ps, 1, id),
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }

    @Override
    public List<OutboxDlqEvent> findBatch(DlqFilter filter, int batchNumber, int batchSize) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at 
            FROM outbox_dlq_events
        """);
        String sqlEnd = " ORDER BY moved_at, id LIMIT ? OFFSET ?";
        List<Object> params = new LinkedList<>();

        buildQuery(filter, sql, params);
        sql.append(sqlEnd);
        params.add(batchSize);
        params.add(batchNumber * batchSize);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapper.toDlqEvent(rs), params.toArray(new Object[0]));
    }

    @Override
    public long count(DlqFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM outbox_dlq_events");
        List<Object> params = new LinkedList<>();
        buildQuery(filter, sql, params);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray(new Object[0]));
        return count == null ? 0 : count;
    }

    protected void buildQuery(DlqFilter filter, StringBuilder sqlBuilder, List<Object> params) {
        boolean hasWhereClause = false;
        if (filter.hasStatus()) {
            sqlBuilder.append(" WHERE dlq_status = ?");
            hasWhereClause = true;
            params.add(filter.getStatus().name());
        }
        if (filter.hasEventType()) {
            sqlBuilder.append(hasWhereClause ? " AND" : " WHERE")
                    .append(" event_type = ?");
            params.add(filter.getEventType());
        }
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        jdbcTemplate.update(
                "UPDATE outbox_dlq_events SET dlq_status = ?, updated_at = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 3, id);
                }
        );
    }

    @Override
    public int updateBatchStatus(DlqFilter filter, DlqStatus forbiddenStatus) {
        if (!filter.hasStatus()) {
            throw new InvalidDlqFilterException("Filter must have new event status for update");
        }
        StringBuilder sql = new StringBuilder("""
            UPDATE outbox_dlq_events 
            SET dlq_status = ?, updated_at = ? 
            WHERE dlq_status != ?
        """);

        List<Object> params = new LinkedList<>();
        params.add(filter.getStatus().name());
        params.add(Timestamp.from(clock.instant()));
        params.add(forbiddenStatus.name());

        buildModificationQuery(filter, sql, params);
        return jdbcTemplate.update(sql.toString(), params.toArray(new Object[0]));
    }

    @Override
    public int deleteById(UUID id) {
        String sql = "DELETE FROM outbox_dlq_events WHERE id = ?";
        return jdbcTemplate.update(sql, ps -> uuidHelper.setToPs(ps, 1, id));
    }

    @Override
    public int deleteBatch(DlqFilter filter, DlqStatus forbiddenStatus) {
        StringBuilder sql = new StringBuilder("""
            DELETE FROM outbox_dlq_events 
            WHERE dlq_status != ?
        """);

        List<Object> params = new LinkedList<>();
        params.add(forbiddenStatus.name());

        buildModificationQuery(filter, sql, params);
        return jdbcTemplate.update(sql.toString(), params.toArray(new Object[0]));
    }

    protected void buildModificationQuery(DlqFilter filter, StringBuilder sqlBuilder, List<Object> params) {
        if (filter.hasEventType()) {
            sqlBuilder.append(" AND event_type = ?");
            params.add(filter.getEventType());
        } else if (filter.hasIds()) {
            Set<UUID> ids = filter.getIds();
            if (!RepositoryUtils.isIdsValid(ids)) {
                throw new InvalidDlqFilterException("Passed ids is null or empty");
            }
            sqlBuilder.append(" AND id IN (%s)".formatted(RepositoryUtils.generateIdsPlaceholders(ids)));
            for (UUID id : ids) {
                params.add(convertIdParameter(id));
            }
        } else {
            throw new InvalidDlqFilterException("Filter hasn't both params");
        }
    }

    protected abstract Object convertIdParameter(UUID id);
}
