package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.inbox.ConcurrentInsertException;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.*;

public class MySqlInboxRepository implements InboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final BytesSqlUuidHelper uuidHelper;
    protected final BytesResultSetMapper mapper;

    public MySqlInboxRepository(JdbcTemplate jdbcTemplate,
                                BytesSqlUuidHelper uuidHelper,
                                BytesResultSetMapper mapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT IGNORE INTO inbox_events (id)
            VALUES(?)
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
            INSERT IGNORE INTO inbox_events(id)
            VALUES %s
        """.formatted(RepositoryUtils.generateValuesPlaceholders(nonExistsIds.size(), 1));
        int updatedRows = jdbcTemplate.update(
                insertSql,
                ps -> {
                    int paramId = 1;
                    for (UUID id: nonExistsIds) {
                        ps.setBytes(paramId++, uuidHelper.uuidToBytes(id));
                    }
                }
        );

        if (nonExistsIds.size() != updatedRows) {
            throw new ConcurrentInsertException(nonExistsIds.size(), updatedRows, nonExistsIds);
        }

        return new HashSet<>(nonExistsIds);
    }

    @Override
    public int deleteBatchOlderThan(Duration age, int batchSize) {
        String sql = """
            DELETE FROM inbox_events
            WHERE consumed_at <= UTC_TIMESTAMP(6) - INTERVAL (? * 1000) MICROSECOND
            ORDER BY consumed_at
            LIMIT ?
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setLong(1, age.toMillis());
                    ps.setInt(2, batchSize);
                }
        );
    }
}
