package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConcurrentInsertException;
import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class MySqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final Clock clock;
    protected final BytesSqlUuidHelper uuidHelper;
    protected final BytesResultSetMapper mapper;

    public MySqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate,
                                         Clock clock,
                                         BytesSqlUuidHelper uuidHelper,
                                         BytesResultSetMapper mapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT IGNORE INTO outbox_consumed_events (id, consumed_at)
            VALUES(?, ?)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    uuidHelper.setToPs(ps, 1, id);
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                }
        );
    }

    @Override
    public Set<UUID> saveIfAbsent(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptySet();
        }

        String existsIdsSql = """
            SELECT id 
            FROM outbox_consumed_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        List<UUID> existsIds = jdbcTemplate.query(
                existsIdsSql,
                ps -> uuidHelper.setToPs(ps, 1, ids),
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id"))
        );

        Set<UUID> nonExistsIds = new HashSet<>(ids);
        nonExistsIds.removeAll(new HashSet<>(existsIds));

        if (!RepositoryUtils.isIdsValid(nonExistsIds)) {
            return Collections.emptySet();
        }

        String insertSql = """
            INSERT IGNORE INTO outbox_consumed_events(id, consumed_at)
            VALUES %s
        """.formatted(RepositoryUtils.generateValuesPlaceholders(nonExistsIds.size(), 2));
        Instant consumedAt = clock.instant();
        int updatedRows = jdbcTemplate.update(
                insertSql,
                ps -> {
                    int paramId = 1;
                    for (UUID id: nonExistsIds) {
                        ps.setBytes(paramId++, uuidHelper.uuidToBytes(id));
                        ps.setTimestamp(paramId++, Timestamp.from(consumedAt));
                    }
                }
        );

        if (nonExistsIds.size() != updatedRows) {
            throw new ConcurrentInsertException(nonExistsIds.size(), updatedRows, nonExistsIds);
        }

        return new HashSet<>(nonExistsIds);
    }

    @Override
    public int deleteBatchByThreshold(Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_consumed_events
            WHERE consumed_at <= ?
            ORDER BY consumed_at
            LIMIT ?
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(threshold));
                    ps.setInt(2, batchSize);
                }
        );
    }
}
