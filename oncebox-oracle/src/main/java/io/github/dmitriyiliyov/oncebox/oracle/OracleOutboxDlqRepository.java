package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class OracleOutboxDlqRepository extends AbstractOutboxDlqRepository {

    private final BytesResultSetMapper localMapper;

    public OracleOutboxDlqRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper, BytesResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper, mapper);
        this.localMapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            ORDER BY moved_at
            FOR UPDATE SKIP LOCKED
        """;

        List<OutboxDlqEvent> events = jdbcTemplate.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(selectSql);
                    ps.setMaxRows(batchSize);
                    ps.setString(1, status.name());
                    return ps;
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
                SET dlq_status = ?, updated_at = SYSTIMESTAMP
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
        String selectSql = """
            SELECT id FROM outbox_dlq_events
            WHERE dlq_status = ? AND updated_at <= SYSTIMESTAMP - NUMTODSINTERVAL(? / 1000, 'SECOND')
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
                (rs, numRow) -> localMapper.fromBytesToUuid(rs.getBytes("id"))
        ));

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String sql = """
            DELETE FROM outbox_dlq_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}
