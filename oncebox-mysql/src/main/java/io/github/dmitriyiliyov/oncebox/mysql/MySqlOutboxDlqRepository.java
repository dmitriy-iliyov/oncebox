package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MySqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    public MySqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper, BytesResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper, mapper);
    }

    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            ORDER BY moved_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxDlqEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );

        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());

        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }

        String lockSql = """
            UPDATE outbox_dlq_events 
                SET dlq_status = ?, updated_at = UTC_TIMESTAMP(6)
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    uuidHelper.setToPs(ps, 2, ids);
                }
        );

        events.forEach(event -> event.setDlqStatus(lockStatus));
        return events;
    }

    @Override
    public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) {
        String sql = """
            DELETE FROM outbox_dlq_events
            WHERE dlq_status = ? AND updated_at <= UTC_TIMESTAMP(6) - INTERVAL (? * 1000) MICROSECOND
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

    /**
     * Binds the moment as UTC wall time: a {@code DATETIME} column keeps no time zone.
     */
    @Override
    protected void setInstant(PreparedStatement ps, int index, Instant instant) throws SQLException {
        ps.setObject(index, LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
