package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.*;

public class PostgreSqlInboxRepository implements InboxRepository {

    protected final JdbcTemplate jdbcTemplate;

    public PostgreSqlInboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT INTO inbox_events (id)
            VALUES(?)
            ON CONFLICT (id) DO NOTHING
        """;
        return jdbcTemplate.update(
                sql,
                ps -> ps.setObject(1, id)
        );
    }

    @Override
    public Set<UUID> saveIfAbsent(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptySet();
        }

        String sql = """
                INSERT INTO inbox_events (id) 
                VALUES %s 
                ON CONFLICT (id) DO NOTHING
                RETURNING id
        """.formatted(RepositoryUtils.generateValuesPlaceholders(ids.size(), 1));

        return new HashSet<>(
                jdbcTemplate.query(
                        sql,
                        ps -> {
                            int paramId = 1;
                            for (UUID id : ids) {
                                ps.setObject(paramId++, id);
                            }
                        },
                        (rs, rowNum) -> rs.getObject("id", UUID.class)
                )
        );
    }

    @Override
    public int deleteBatchOlderThan(Duration age, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id 
                FROM inbox_events 
                WHERE consumed_at <= clock_timestamp() - (? * INTERVAL '1 millisecond')
                ORDER BY consumed_at
                LIMIT ?
            )
            DELETE FROM inbox_events
            WHERE id IN (SELECT id FROM to_delete)
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
