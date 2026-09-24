package io.github.dmitriyiliyov.oncebox.oracle;

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

public class OracleConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final Clock clock;
    protected final BytesSqlUuidHelper uuidHelper;
    protected final BytesResultSetMapper mapper;

    public OracleConsumedOutboxRepository(JdbcTemplate jdbcTemplate,
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
            MERGE INTO outbox_consumed_events t
            USING (
                SELECT ? AS id, ? AS consumed_at FROM dual
            ) v_t
            ON (t.id = v_t.id)
            WHEN NOT MATCHED THEN
                INSERT (id, consumed_at)
                VALUES (v_t.id, v_t.consumed_at);
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
            INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(outbox_consumed_events (id)) */
            INTO outbox_consumed_events (id, consumed_at)
            VALUES (?, ?)
        """;

        Instant consumedAt = clock.instant();
        int [][] batchUpdateResult = jdbcTemplate.batchUpdate(
                insertSql,
                nonExistsIds,
                nonExistsIds.size(),
                (ps, id) -> {
                    ps.setBytes(1, uuidHelper.uuidToBytes(id));
                    ps.setTimestamp(2, Timestamp.from(consumedAt));
                }
        );
        int updRowsCount = Arrays.stream(batchUpdateResult)
                .flatMapToInt(Arrays::stream)
                .sum();

        if (nonExistsIds.size() != updRowsCount) {
            throw new ConcurrentInsertException(nonExistsIds.size(), updRowsCount, nonExistsIds);
        }

        return nonExistsIds;
    }

    @Override
    public int deleteBatchByThreshold(Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id
            FROM outbox_consumed_events
            WHERE consumed_at <= ?
            ORDER BY consumed_at
            FETCH FIRST ? ROWS ONLY
        """;

        Set<UUID> ids = new HashSet<>(
                jdbcTemplate.query(
                        selectSql,
                        ps -> {
                            ps.setTimestamp(1, Timestamp.from(threshold));
                            ps.setInt(2, batchSize);
                        },
                        (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id"))
                )
        );

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String deleteSql = """
            DELETE FROM outbox_consumed_events
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(deleteSql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}
