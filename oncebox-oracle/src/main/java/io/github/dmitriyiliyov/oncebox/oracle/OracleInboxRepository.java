package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.inbox.ConcurrentInsertException;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.*;

public class OracleInboxRepository implements InboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final BytesSqlUuidHelper uuidHelper;
    protected final BytesResultSetMapper mapper;

    public OracleInboxRepository(JdbcTemplate jdbcTemplate,
                                 BytesSqlUuidHelper uuidHelper,
                                 BytesResultSetMapper mapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            MERGE INTO inbox_events t
            USING (
                SELECT ? AS id FROM dual
            ) v_t
            ON (t.id = v_t.id)
            WHEN NOT MATCHED THEN
                INSERT (id)
                VALUES (v_t.id);
        """;
        return jdbcTemplate.update(
                sql,
                ps -> uuidHelper.setToPs(ps, 1, id)
        );
    }

    @Override
    public Set<UUID> saveIfAbsent(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptySet();
        }

        String existsIdsSql = """
            SELECT id 
            FROM inbox_events
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
            INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(inbox_events (id)) */
            INTO inbox_events (id)
            VALUES (?)
        """;

        int [][] batchUpdateResult = jdbcTemplate.batchUpdate(
                insertSql,
                nonExistsIds,
                nonExistsIds.size(),
                (ps, id) -> ps.setBytes(1, uuidHelper.uuidToBytes(id))
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
    public int deleteBatchOlderThan(Duration age, int batchSize) {
        String selectSql = """
            SELECT id
            FROM inbox_events
            WHERE consumed_at <= SYSTIMESTAMP - NUMTODSINTERVAL(? / 1000, 'SECOND')
            ORDER BY consumed_at
            FETCH FIRST ? ROWS ONLY
        """;

        Set<UUID> ids = new HashSet<>(
                jdbcTemplate.query(
                        selectSql,
                        ps -> {
                            ps.setLong(1, age.toMillis());
                            ps.setInt(2, batchSize);
                        },
                        (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id"))
                )
        );

        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }

        String deleteSql = """
            DELETE FROM inbox_events
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(deleteSql, ps -> uuidHelper.setToPs(ps, 1, ids));
    }
}
