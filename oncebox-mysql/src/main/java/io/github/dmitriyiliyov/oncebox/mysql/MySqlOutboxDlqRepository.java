package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MySqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    private final Clock clock;

    public MySqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper, BytesResultSetMapper mapper,
                                    Clock clock) {
        super(jdbcTemplate, uuidHelper, mapper);
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
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
                SET dlq_status = ?, updated_at = ?
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    uuidHelper.setToPs(ps, 3, ids);
                }
        );

        events.forEach(event -> event.setDlqStatus(lockStatus));
        return events;
    }

    @Override
    public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_dlq_events
            WHERE dlq_status = ? AND updated_at <= ?
            ORDER BY updated_at
            LIMIT ?
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
